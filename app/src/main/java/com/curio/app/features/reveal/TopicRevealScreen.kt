package com.curio.app.features.reveal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioPassport
import com.curio.app.data.CurioPet
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioTopic
import com.curio.app.data.ExploreReminderScheduler
import com.curio.app.data.ExploreSession
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.TourController
import com.curio.app.data.TopicCatalog
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.buildExploreSearchUrl
import com.curio.app.data.buildGoogleSearchUrl
import com.curio.app.data.buildYouTubeSearchUrl
import com.curio.app.data.categoryOpensYouTube
import com.curio.app.data.openSilentExplore
import com.curio.app.infrastructure.ExploreSessionService
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.LocalRevealSharedScope
import com.curio.app.ui.adaptive.LocalRevealVisibilityScope
import com.curio.app.ui.adaptive.RevealBoundsTransform
import com.curio.app.ui.adaptive.RevealSharedElementKey
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.SoftTornBottomShape
import com.curio.app.ui.components.categoryEdgeShine
import com.curio.app.ui.components.curioButtonColors
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioGradients
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryBackgroundWash
import com.curio.app.ui.theme.categoryBorder
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.themedAccent
import com.curio.app.ui.theme.themedButtonFill
import com.curio.app.ui.theme.themedButtonInk

/**
 * Topic Reveal — see Curio reveal contract.
 *
 * Upgraded from the previous §6 design:
 *  - Gradient-ticket hero header card (260dp) matching the Spin screen:
 *    accent → DeepPlum vertical gradient (rainbow for wildcard), white
 *    watermark glyph, white pill badges ("verb + duration" top-left,
 *    subtype bottom-right).
 *  - Hero shows the action you need to take immediately — the verb +
 *    duration badge sits on the ticket, not buried under the body copy.
 *  - Bigger, eye-catching topic name (uses the geom typography).
 *  - Tags row immediately under the title — gives instant context for
 *    genres / eras (e.g. "1970s · British · Art Rock").
 *  - Existing teaser card + explore-action prompt card are preserved.
 *  - Refined spacing — top padding tight (statusBarsPadding + 8dp.
 *
 * Layout, top → bottom:
 *   24-44 dp   statusBarsPadding()
 *   40 dp      Top bar (close ✕ → Pop back to the Spin deck)
 *    8 dp      gap
 *   ~260 dp    Hero card (gradient ticket: watermark glyph + badges +
 *              the topic NAME — the title lives on the card so the
 *              shared-element morph grows it in place, v8.25)
 *   20 dp      gap
 *   ~42 dp     Tags chip row
 *   20 dp      gap
 *   ~auto     "One quirky fact to get you curious" card
 *   16 dp      gap
 *   ~auto     "{verb} {target}" action prompt card + "~N min"
 *   24 dp      content breathing room
 *   below hero  inline actions (Start exploring + Already …)
 */

// v8.xx — the reveal renders its OWN torn paper edge at the bottom of the
// screen (at navbar height) instead of the bottom navigation bar — the
// mirror of the hero's downward tear: this strip tears UP at its top edge
// (see CurioNavHost.showBottomBar). Fixed seed → the seam never re-rolls.
private const val REVEAL_BOTTOM_TEAR_SEED = 0xB07E4
private val RevealBottomTearHeight = 80.dp

@Composable
fun TopicRevealScreen(
    categorySlug: String,
    topicName: String,
    navController: NavController,
    // Browse-Topics mode (see CurioRoutes.REVEAL): Explore is silent and
    // feedback/recents are disabled, while Express Yourself intentionally
    // remains available as the explicit write path.
    browseMode: Boolean = false
) {
    val cat = remember(categorySlug) {
        CurioCategories.byRouteSlug(categorySlug)
            ?: CurioCategories.byId(CategoryId.WILDCARD)
    }

    val topic by produceState<CurioTopic?>(initialValue = null, topicName, cat.id) {
        val cached = TopicCatalog.findByName(topicName)
        if (cached != null) {
            value = cached
            return@produceState
        }
        val pool = TopicJsonLoader.load(cat.id)
        // Graceful fallback: an unknown topic stays null so the screen
        // shows the neutral category fallback instead of a wrong topic.
        value = pool.firstOrNull { it.name == topicName }
    }

    val resolved = topic
    val context = LocalContext.current
    // v6.7 — pin for later: the bookmark toggles on/off so the user can save
    // the topic and revisit it from Topic History → "Pinned for later".
    // Reads the REACTIVE pinnedTopicsState (not prefs) so the icon toggles
    // immediately when the user taps pin/unpin.
    val isPinned = resolved != null &&
        AppPreferences.pinnedTopicsState.any { it.categoryId == cat.id && it.topicName == resolved.name }
    // v7.92 — reactive done state: the "Already …" button flips to a filled
    // marked state once the topic is done, and the unwatch action appears.
    // Reads doneTopicsState inside composition, so it updates the moment
    // markDone / unmarkDone changes the set.
    // v7 — like/dislike teaches the shuffle: liked topics (and their whole
    // category) get more weight, disliked get less — never fully blocked.
    // Reads the REACTIVE sentiment state so the buttons toggle instantly.
    val sentiment = resolved?.let { AppPreferences.topicSentiment(cat.id, it.id) }

    // v8.5 — Category passport: opening a reveal counts as a "peek" for the
    // lane's stamp and refreshes its last-explored date (spec §6.1). Fires
    // once per topic opened; a rotation re-fire is harmless (the stamp
    // derives from counts > 0, never exact values).
    // v8.6 — the First Journey tour's "Open the landed topic" step advances
    // the moment the reveal really opens (spec §7.2 step 5).
    LaunchedEffect(cat.id, resolved?.id, browseMode) {
        if (resolved != null && !browseMode) {
            CurioPassport.noteReveal(context, cat.id)
            // v8.30 — the pet reacts to the REAL cause: the spin's auto-open
            // says "it opened itself"; any user tap gets a touch reaction
            // ("You picked it!") instead of claiming it auto-opened.
            val auto = CurioPet.consumeRevealAuto()
            CurioPet.reactTo(
                if (auto) CurioPet.Event.REVEAL_AUTO else CurioPet.Event.REVEAL_TAPPED
            )
        }
    }

    // Explore-session flow — tapping the CTA records the topic as
    // recently-explored the moment it's tapped (even before anything is
    // saved to the Cabinet), then opens a two-way dialog (Explore now /
    // Write about it). Leaving the screen without engaging records it as
    // recently-unexplored so Home can offer to resume it.
    var engaged by rememberSaveable { mutableStateOf(false) }
    var showExploreDialog by rememberSaveable { mutableStateOf(false) }

    val latestBrowseMode by rememberUpdatedState(browseMode)
    val latestResolved by rememberUpdatedState(resolved)
    val latestOnExplore by rememberUpdatedState<() -> Unit> {
        // Explore is not a tour stop. During the tour, tapping it only exits
        // the guide; it must not open a dialog/browser or navigate to a stale
        // route. Normal taps continue into the real flow.
        if (TourController.active) {
            TourController.skip()
        } else {
            showExploreDialog = true
        }
    }
    // v8.12 — browse-mode (opened from the Topic Database) gets a SILENT
    // Explore action: it opens the topic's search page without recording
    // quests, passport, pet events, recents or a timer. Express Yourself is
    // separate and remains the deliberate write-about-it path.
    val latestOnSilentExplore by rememberUpdatedState<() -> Unit> {
        // Browse mode has no tour navigation. During a tour, only dismiss the
        // guide; never launch a browser as a side effect of a demonstrated tap.
        if (TourController.active) {
            TourController.skip()
        } else {
            latestResolved?.let { topic -> openSilentExplore(context, topic) }
        }
    }
    val latestOnAlready by rememberUpdatedState<() -> Unit> {
        if (TourController.active) {
            // Tour taps demonstrate controls only. End the tour instead of
            // opening the capture task or navigating to a stale next route.
            TourController.skip()
        } else {
            latestResolved?.let { topic ->
                engaged = true
                navController.navigate(CurioRoutes.captureFor(cat.id.routeSlug, topic.name)) {
                    launchSingleTop = true
                }
            }
        }
    }
    // v10 — Topic Reveal owns its content edge-to-edge. The old transparent
    // bottom Scaffold slot was only a layout reservation and could leave the
    // screen with stale bottom padding during navigation.

    // Android 13+ needs POST_NOTIFICATIONS before the persistent explore
    // notification can show — requested when the user starts exploring with
    // live notifications on (the session, bubble and reminder work either way).
    // Plain `remember` (not saveable): a rotation mid-dialog drops the
    // continuation, but the session is already persisted and the user can
    // simply tap "Explore now" again.
    var pendingNotificationSession by remember { mutableStateOf<ExploreSession?>(null) }

    /** Opens the search page (Google — YouTube for music), then lands back
     *  on Home — returning to the
     *  app triggers the "are you done exploring?" prompt. Deferred into the
     *  permission callback when a notification-permission request is in
     *  flight, so the foreground service starts while this activity is still
     *  foreground (a background FGS start throws on Android 12+). */
    fun openExploreBrowserAndGoHome(session: ExploreSession) {
        showExploreDialog = false
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(session.searchUrl)))
        }
        navController.navigate(CurioRoutes.HOME) {
            popUpTo(CurioRoutes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        val pending = pendingNotificationSession
        pendingNotificationSession = null
        if (pending != null) {
            // Start the service for whatever can show right now — the live
            // notification needs the grant, but the FLOATING BUBBLE needs
            // only the separate "Display over other apps" permission, so
            // denying POST_NOTIFICATIONS must never silently kill the
            // bubble too. The service's render() picks what actually shows
            // from the current permission state. The browser hasn't opened
            // yet (proceed is deferred to here), so the activity is still
            // foreground — starting the foreground service is allowed.
            if (AppPreferences.exploreServiceShouldRun(context)) {
                ExploreSessionService.start(context, pending)
            }
            openExploreBrowserAndGoHome(pending)
        }
    }

    // ── Floating explore bubble permission ────────────────────────���───
    //    "Display over other apps" has no runtime dialog on Android 10+, so
    //    Allow opens the system special-access page; ON_RESUME below resumes
    //    the deferred flow (and starts the bubble service if granted). Asked
    //    whenever the permission is missing — never a one-time gate.
    //    Plain `remember` (not saveable): a rotation mid-dialog drops the
    //    continuation, but the session is already persisted and the user can
    //    simply tap "Explore now" again.
    var pendingOverlaySession by remember { mutableStateOf<ExploreSession?>(null) }
    var overlayNeedsNotification by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by rememberSaveable { mutableStateOf(false) }
    // Only consume the pending session after the app has actually launched
    // the system overlay-settings page. A dialog dismissal can produce an
    // ON_RESUME callback while permission is still missing; consuming here
    // would open the browser and clear the pending handoff before the user
    // grants the permission, leaving the service never started.
    var awaitingOverlaySettings by remember { mutableStateOf(false) }

    // ── Active-session conflict — starting a new explore while another is
    //    running must ASK first (Save for later / Explore now) instead of
    //    silently discarding the running session. Plain `remember`: a
    //    rotation drops the continuation, and the running session is safe
    //    either way (nothing is started until the dialog resolves).
    var conflictActiveSession by remember { mutableStateOf<ExploreSession?>(null) }
    var pendingConflictSession by remember { mutableStateOf<ExploreSession?>(null) }
    var showConflictDialog by rememberSaveable { mutableStateOf(false) }

    /** Continues the explore flow after the overlay-permission step resolves. */
    fun continueExploreFlow(session: ExploreSession) {
        // Same gate as beginExploreSession: once the overlay permission is
        // granted the floating bubble will show, so the POST_NOTIFICATIONS
        // prompt is skipped — the bubble carries the timer. Only ask when
        // the bubble can't show and a live notification is actually wanted
        // (the shade notification is then the only timer controller).
        // v7.35 — [AppPreferences.overlayActuallyUsable] (not raw
        // canDrawOverlays): an Android 15+ pending grant reports true but
        // never shows the overlay, so the bubble must not be "expected"
        // until the AppOps state settles.
        val bubbleWillShow = AppPreferences.isOverlayBubbleEnabled(context) &&
            AppPreferences.overlayActuallyUsable(context)
        if (overlayNeedsNotification &&
            AppPreferences.isLiveNotificationsEnabled(context) &&
            !hasNotificationPermission(context) && !bubbleWillShow
        ) {
            pendingNotificationSession = session
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openExploreBrowserAndGoHome(session)
        }
    }

    // When the user returns from the "Display over other apps" settings page
    // (opened by the overlay prompt), resume the deferred flow and start the
    // bubble service if the permission was granted.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingOverlaySettings) {
                awaitingOverlaySettings = false
                val pending = pendingOverlaySession
                pendingOverlaySession = null
                if (pending != null) {
                    if (AppPreferences.overlayActuallyUsable(context)) {
                        // Re-arm while this Activity is foreground, then let
                        // the normal flow move to the browser/Home. This is
                        // the reliable handoff after special-access settings.
                        ExploreSessionService.start(context, pending)
                        // A fresh grant re-opens the door for future asks.
                        AppPreferences.setOverlayAskDeclined(context, false)
                    } else {
                        // v8.1 — returned from system settings WITHOUT
                        // granting: that's a "no" — record it so the prompt
                        // never re-asks on every explore.
                        AppPreferences.setOverlayAskDeclined(context, true)
                    }
                    continueExploreFlow(pending)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /**
     * Starts [session] once the conflict check has passed. Declared BEFORE
     * startExploreSession because Kotlin local functions are scoped from
     * their declaration point onward — a forward reference from
     * startExploreSession would be an unresolved reference at compile time.
     */
    fun beginExploreSession(session: ExploreSession) {
        if (AppPreferences.isExploreSessionsEnabled(context)) {
            ExploreSessionStore.startSession(context, session)
            // Reminder always — fires even without the live notification
            // (live notifications off → no foreground service to arm it).
            ExploreReminderScheduler.schedule(context, session.startMillis, session.durationMinutes)
        }
        val needsOverlay = AppPreferences.isOverlayBubbleEnabled(context) &&
            !AppPreferences.overlayActuallyUsable(context) &&
            // v8.1 — once the user says no, stop asking: the explore
            // proceeds without the bubble and the Settings toggle is the
            // only way back in.
            !AppPreferences.isOverlayAskDeclined(context)
        // The floating bubble shows the same live timer over other apps and
        // needs ONLY the "Display over other apps" permission. When it's
        // going to show, skip the POST_NOTIFICATIONS prompt — a live shade
        // notification would be redundant while the bubble is up, and
        // re-asking after a denial is a nag. The notification is only worth
        // asking for when the bubble is off or its permission is missing
        // (the shade notification is then the only timer controller).
        val bubbleWillShow = AppPreferences.isOverlayBubbleEnabled(context) &&
            AppPreferences.overlayActuallyUsable(context)
        val needsNotification = AppPreferences.isLiveNotificationsEnabled(context) &&
            !hasNotificationPermission(context) && !bubbleWillShow

        if (needsOverlay) {
            // The bubble floats over other apps and needs the "Display over
            // other apps" special access — ask whenever it's missing (not a
            // one-time ask; "Not now" proceeds without the bubble and the
            // prompt returns on the next session). Defer the browser until
            // the user answers (Allow → system settings → ON_RESUME).
            overlayNeedsNotification = needsNotification
            pendingOverlaySession = session
            showOverlayPermissionDialog = true
            return
        }
        if (needsNotification) {
            // Ask for POST_NOTIFICATIONS first (Android 13+ hides the
            // notification without it). The permission callback starts the
            // service — while this activity is still foreground — and then
            // opens the browser + Home, so no background FGS start (which
            // throws on Android 12+).
            pendingNotificationSession = session
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        // Start the service for whatever can show right now (bubble and/or
        // live notification); the permission paths above defer their start
        // to their callbacks.
        if (AppPreferences.exploreServiceShouldRun(context)) {
            ExploreSessionService.start(context, session)
        }
        openExploreBrowserAndGoHome(session)
    }

    /** Starts a timed explore session, opens the search page (Google — YouTube for music), back to Home. */
    fun startExploreSession(topic: CurioTopic, searchUrl: String = buildExploreSearchUrl(topic)) {
        engaged = true
        // Engaging for real — record as recently-explored and clear any
        // recently-unexplored entry. recordExplored tags the row "Resumed"
        // when the user came back to a topic they'd left.
        ExploreSessionStore.recordExplored(context, cat.id, topic.name)
        ExploreSessionStore.removeUnexplored(context, cat.id, topic.name)
        val action = topic.exploreAction
        val session = ExploreSession(
            categoryId = cat.id,
            topicName = topic.name,
            subtype = topic.subtype,
            verb = action.verb,
            targetName = action.targetName,
            durationMinutes = action.durationMinutes,
            instruction = action.instruction,
            searchUrl = searchUrl,
            startMillis = System.currentTimeMillis()
        )
        // Starting a new explore while another session is running would
        // silently discard it — ask first instead (Save for later / Explore
        // now). Same-topic restarts are allowed to proceed straight through.
        val active = ExploreSessionStore.getActiveSession(context)
        if (active != null && active.topicName != topic.name) {
            conflictActiveSession = active
            pendingConflictSession = session
            showConflictDialog = true
            return
        }
        beginExploreSession(session)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Category tint wash — the reveal page wears a faint wash of the
            // topic's category over the theme background, matching the Spin
            // page so the whole explore flow feels tied to the deck.
            // Theme-aware: deep accent over cream in light, pastel twin glow
            // over midnight in dark (deep accents look muddy on dark).
            .background(cat.categoryBackgroundWash())
    ) {
        // ── Watermark backdrop — every category glyph scattered behind the
        //    content (FIXED — the content scrolls over it), the same
        //    backdrop language as Home / Spin / the saved-entry page. The
        //    teaser / action cards above it sit on OPAQUE category surfaces
        //    so the glyphs only show in the gaps around them, never bleeding
        //    through the cards.
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            // The NavHost reserves a navbar-height placeholder for Reveal,
            // so this backdrop can fill the same content bounds as the Spin
            // tab. Keeping the whole destination's bounds stable fixes both
            // the watermark level and the shared-card morph target.
            CurioWatermarkBackdrop(activeCat = cat)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
        // ── 1. Top bar (pin bookmark + close ✕) ────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pin for later — filled bookmark when pinned (category accent),
            // outline when not. Only meaningful once the topic has resolved.
            Surface(
                onClick = {
                    val topic = resolved ?: return@Surface
                    if (AppPreferences.isTopicPinned(context, cat.id, topic.name)) {
                        AppPreferences.unpinTopic(context, cat.id, topic.name)
                    } else {
                        AppPreferences.pinTopic(context, cat.id, topic.name)
                    }
                },
                shape = CircleShape,
                color = if (isPinned) cat.themedAccent() else MaterialTheme.colorScheme.surfaceVariant
            ) {
                CurioIcon(
                    name = if (isPinned) CurioIcons.Bookmark else CurioIcons.BookmarkBorder,
                    contentDescription = if (isPinned) "Unpin this topic" else "Pin this topic for later",
                    tint = if (isPinned) cat.onAccent() else MaterialTheme.colorScheme.onSurface,
                    size = 22.dp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Close — return to the Spin deck (not Home): the landed card
            // keeps its "Tap to open" state so it can be reopened until the
            // user spins again or explores it (v5.6).
            Surface(
                onClick = {
                    // Browse mode is read-only: nothing is ever recorded.
                    if (!browseMode && !engaged) {
                        resolved?.let { ExploreSessionStore.recordUnexplored(context, cat.id, it.name) }
                    }
                    navController.popBackStack()
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                CurioIcon(
                    name = CurioIcons.Close,
                    contentDescription = "Close and return to the deck",
                    tint = MaterialTheme.colorScheme.onSurface,
                    size = 22.dp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // ── 2. Hero card — category watermark + verb/duration badge ──
                // The hero is a SHARED ELEMENT matching the Spin front ticket
                // ("reveal-hero"): opening a landed topic expands the hero
                // out of the card's position instead of sliding the page in.
                // The destination's AnimatedVisibilityScope drives its
                // visibility during the route transition, so no separate
                // MorphEntrance wrapper (it would double-animate the hero).
                // Key the hero to the actual topic, not the initial loading
                // state, so the shared element re-mounts per topic.
                key(resolved?.id ?: "topic-loading") {
                    HeroCard(
                        cat = cat,
                        resolved = resolved,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                            // ── 2.5 Action row — Express yourself / Explore ─────────
                // v8.57 — the actions moved OUT of the bottom dock to sit
                // right below the hero card: always visible, no scaffold.
                RevealContentEntrance(delayMillis = 40) {
                    RevealActionRow(
                        cat = cat,
                        browseMode = latestBrowseMode,
                        resolved = latestResolved,
                        onExplore = latestOnExplore,
                        onAlready = latestOnAlready,
                        onSilentExplore = if (latestBrowseMode) latestOnSilentExplore else null,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                // ── 3. Topic name ───────────────────────────────────────────
                // v8.25 — the topic name now lives INSIDE the hero card
                // (see HeroCard above), so it morphs with the card instead
                // of popping in below as a separate headline: opening a
                // landed topic grows the title in place with the rest of
                // the card. The tags row below simply follows the hero
                // directly.

                // ── 5. Teaser card ──────────────────────────────────────────
                RevealContentEntrance(delayMillis = 160) {
                    TeaserCard(
                        cat = cat,
                        teaser = resolved?.teaser,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                }

                // ── 6. Action prompt card ──────────────────────────────────
                if (resolved != null) {
                    RevealContentEntrance(delayMillis = 210) {
                        ActionPromptCard(
                            cat = cat,
                            action = resolved.exploreAction,
                            subtype = resolved.subtype,
                            modifier = Modifier.padding(top = 14.dp)
                        )
                    }
                }

                // ── 6.5 Like / dislike — feeds the shuffle weighting ──
                // Hidden in Browse-Topics mode: reading from the database
                // must not shape the shuffle (pure read-only).
                if (!browseMode && resolved != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SentimentButton(
                            icon = CurioIcons.ThumbDown,
                            label = "Dislike",
                            active = sentiment == AppPreferences.SENTIMENT_DISLIKE,
                            accent = cat.themedAccent(),
                            ink = cat.onAccent(),
                            onClick = {
                                AppPreferences.setTopicSentiment(
                                    context, cat.id, resolved.id,
                                    if (sentiment == AppPreferences.SENTIMENT_DISLIKE)
                                        AppPreferences.SENTIMENT_NONE
                                    else AppPreferences.SENTIMENT_DISLIKE
                                )
                            }
                        )
                        SentimentButton(
                            icon = CurioIcons.ThumbUp,
                            label = "Like",
                            active = sentiment == AppPreferences.SENTIMENT_LIKE,
                            accent = cat.themedAccent(),
                            ink = cat.onAccent(),
                            onClick = {
                                AppPreferences.setTopicSentiment(
                                    context, cat.id, resolved.id,
                                    if (sentiment == AppPreferences.SENTIMENT_LIKE)
                                        AppPreferences.SENTIMENT_NONE
                                    else AppPreferences.SENTIMENT_LIKE
                                )
                            }
                        )
                    }
                }

                // Bottom clearance — the torn paper edge (at navbar height)
                // overlays the very bottom of the scroll area, so the last
                // row clears the seam when fully scrolled down.
                Spacer(Modifier.height(RevealBottomTearHeight + 24.dp))
            }

        }

        // ── Bottom torn paper edge — replaces the bottom navigation bar ──
        // The reveal renders its own torn seam at the bottom of the screen
        // (at navbar height) instead of the Scaffold's navigation bar (see
        // CurioNavHost.showBottomBar). It tears UP at its top edge — the
        // mirror of the hero's downward tear — so the page reads as one
        // torn sheet end-to-end. Fixed seed → never re-rolls.
        val bottomTornShape = remember(REVEAL_BOTTOM_TEAR_SEED) {
            SoftTornBottomShape(REVEAL_BOTTOM_TEAR_SEED, bold = true)
        }
        // v9.x — the strip and its tear are fully opaque and follow the active
        // appearance. Curio uses the category surface, Material uses the
        // device surface, and AMOLED stays pure black.
        val tearPaper = when (AppPreferences.themeStyleState) {
            AppPreferences.THEME_STYLE_AMOLED -> MaterialTheme.colorScheme.surface
            AppPreferences.THEME_STYLE_MATERIAL -> MaterialTheme.colorScheme.surfaceContainer
            else -> cat.categorySurface(MaterialTheme.colorScheme.surface)
        }
        val tearInk = MaterialTheme.colorScheme.onSurface
        // v9.x — NavHost reserves the missing navbar footprint for Reveal
        // without drawing the actual bar. This strip is painted down into
        // that reserved 80dp slot plus the system nav inset, ending flush at
        // the physical screen bottom. The torn seam therefore starts where
        // the real navbar would start on Spin, keeping the page bounds,
        // watermark and shared hero morph at the same level.
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = RevealBottomTearHeight + navInset)
                .fillMaxWidth()
                .height(RevealBottomTearHeight + navInset)
                .graphicsLayer { rotationZ = 180f }
                .clip(bottomTornShape)
                .background(tearPaper)
        )

        // Compact context row: tags live in the reserved footer now, keeping
        // the reveal body focused without changing the footer's fixed height.
        if (!resolved?.tags.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = RevealBottomTearHeight + navInset)
                    .fillMaxWidth()
                    .height(RevealBottomTearHeight + navInset)
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = navInset + 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top
            ) {
                resolved.tags.take(3).forEach { tag ->
                    Surface(
                        modifier = Modifier.weight(1f, fill = false),
                        shape = RoundedCornerShape(50),
                        color = cat.themedAccent().copy(alpha = 0.18f),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = tearInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }

    // Leaving via the system back gesture without engaging → recently-unexplored
    // (never in Browse-Topics mode — reading from the database is silent).
    BackHandler {
        if (!browseMode && !engaged) {
            resolved?.let { ExploreSessionStore.recordUnexplored(context, cat.id, it.name) }
        }
        navController.popBackStack()
    }

    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showOverlayPermissionDialog = false
                // v8.1 — dismissing without granting is a "no": record it so
                // the prompt doesn't re-ask on every explore.
                AppPreferences.setOverlayAskDeclined(context, true)
                val s = pendingOverlaySession
                pendingOverlaySession = null
                if (s != null) continueExploreFlow(s)
            },
            title = { Text("Floating explore bubble?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Curio can show a small timer bubble that floats over " +
                        "other apps, even while you're in the browser. It needs " +
                        "the \"Display over other apps\" permission."
                    )
                    Text(
                        "You can also manage it anytime in Settings → Notifications. " +
                        "Choose \"Not now\" and we won't ask again until you turn " +
                        "it on there.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tip: if you've granted this before and the bubble still " +
                        "doesn't appear, toggle \"Display over other apps\" off " +
                        "and on once in system settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showOverlayPermissionDialog = false
                    val s = pendingOverlaySession
                    if (s != null) {
                        val launched = runCatching {
                            // Mark this before launching Settings so the next
                            // ON_RESUME is known to be the settings return,
                            // not a dialog/composition resume.
                            awaitingOverlaySettings = true
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }.isSuccess
                        if (!launched) {
                            awaitingOverlaySettings = false
                            // No handler for the settings intent — don't
                            // leave the flow stuck; continue without it.
                            pendingOverlaySession = null
                            continueExploreFlow(s)
                        }
                    }
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOverlayPermissionDialog = false
                    // v8.1 — "Not now" is a "no": stop re-asking (the
                    // Settings toggle can still grant it anytime).
                    AppPreferences.setOverlayAskDeclined(context, true)
                    val s = pendingOverlaySession
                    pendingOverlaySession = null
                    if (s != null) continueExploreFlow(s)
                }) { Text("Not now") }
            }
        )
    }

    if (showExploreDialog && resolved != null) {
        val topic = resolved
        val action = topic.exploreAction
        AlertDialog(
            onDismissRequest = {
                // A dismiss gesture (tap-outside / back / swipe) with no
                // action picked = "backed out without exploring" — record
                // the topic as recently-unexplored immediately so Home can
                // offer to resume it, instead of only after the user
                // presses back a second time to leave the screen. The
                // "Explore now" / "Write about it" paths set engaged=true
                // before dismissing, so they never trip this.
                if (!engaged) {
                    ExploreSessionStore.recordUnexplored(context, cat.id, topic.name)
                }
                showExploreDialog = false
            },
            title = { Text("Explore ${topic.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Time to ${action.verb.lowercase()} ${action.targetName}: roughly ${action.durationMinutes} min. Choose Google or YouTube to begin.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Your explore gets timed (not a countdown), and when you come back we'll ask if you're done so you can write it down.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    engaged = true
                    showExploreDialog = false
                    startExploreSession(topic, buildGoogleSearchUrl(topic))
                }) { Text("Explore in Google") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        engaged = true
                        showExploreDialog = false
                        startExploreSession(topic, buildYouTubeSearchUrl(topic))
                    }) { Text("Explore in YouTube") }
                    TextButton(onClick = {
                        showExploreDialog = false
                        ExploreSessionStore.recordUnexplored(context, cat.id, topic.name)
                    }) { Text("Not now") }
                }
            }
        )
    }

    // ── Active-session conflict — another explore is running. Save for
    //    later pins the new topic and keeps the current session going;
    //    Explore now queues the running session (paused, resumable from
    //    Home) and starts the new one. Nothing is started until the user
    //    picks an action — the running session is never silently replaced.
    if (showConflictDialog) {
        val old = conflictActiveSession
        val next = pendingConflictSession
        if (old != null && next != null) {
            AlertDialog(
                onDismissRequest = {
                    showConflictDialog = false
                    val s = pendingConflictSession
                    pendingConflictSession = null
                    conflictActiveSession = null
                    if (s != null) {
                        // Backed out of the new explore without starting it —
                        // record it as recently-unexplored (like any other
                        // back-out) and drop the premature explored record.
                        ExploreSessionStore.recordUnexplored(context, s.categoryId, s.topicName)
                        ExploreSessionStore.removeExplored(context, s.categoryId, s.topicName)
                    }
                },
                title = { Text("Already exploring ${old.topicName}?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "You're in the middle of exploring ${old.topicName}. " +
                            "Start exploring ${next.topicName} instead?"
                        )
                        Text(
                            "The current session gets queued: paused with its time banked, " +
                            "resumable anytime from Home. Or save this new topic for later " +
                            "and keep going.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val s = pendingConflictSession
                        showConflictDialog = false
                        pendingConflictSession = null
                        conflictActiveSession = null
                        if (s != null) {
                            // Queue the running session (paused, time banked),
                            // then start the new explore in its place.
                            ExploreReminderScheduler.cancel(context)
                            ExploreSessionStore.queueActiveSession(context)
                            beginExploreSession(s)
                        }
                    }) { Text("Start new explore") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        // Save the new topic for later — the current session
                        // keeps running untouched.
                        val s = pendingConflictSession
                        showConflictDialog = false
                        pendingConflictSession = null
                        conflictActiveSession = null
                        if (s != null) {
                            AppPreferences.pinTopic(context, s.categoryId, s.topicName)
                        }
                    }) { Text("Save for later") }
                }
            )
        }
    }

}

/**
 * Horizontal size tiers for the reveal action pill: NARROW fits ~320dp
 * screens (small devices, split-screen) without ellipsizing the labels,
 * COMPACT covers normal phones, STANDARD keeps the generous tablet look.
 */
private enum class RevealDockTier { NARROW, COMPACT, STANDARD }

/**
 * All pill + button metrics for one [RevealDockTier] and the vertical
 * [tight] squeeze (less than 48dp of strip — 3-button nav, landscape). One
 * shared table so the pill and both buttons resize together instead of
 * squeezing the labels on small screens.
 */
private data class RevealDockMetrics(
    val pillRadius: Dp,
    val pillPadH: Dp,
    val pillPadV: Dp,
    val rowPadH: Dp,
    val rowPadV: Dp,
    val rowGap: Dp,
    val startPadH: Dp,
    val startPadV: Dp,
    val icon: Dp,
    val textSp: TextUnit,
    val gap: Dp,
    val shadow: Dp
)

private fun revealDockMetrics(tier: RevealDockTier, tight: Boolean): RevealDockMetrics {
    val narrow = tier == RevealDockTier.NARROW
    val compact = tier == RevealDockTier.COMPACT
    val vPad = if (tight) 7.dp else if (narrow) 8.dp else if (compact) 10.dp else 12.dp
    return RevealDockMetrics(
        pillRadius = when {
            tight -> 18.dp
            narrow -> 20.dp
            compact -> 22.dp
            else -> 26.dp
        },
        pillPadH = if (narrow) 10.dp else if (compact) 12.dp else 16.dp,
        pillPadV = if (tight) 6.dp else 8.dp,
        rowPadH = if (narrow) 4.dp else if (compact) 6.dp else 10.dp,
        rowPadV = if (tight) 4.dp else 6.dp,
        rowGap = if (narrow) 6.dp else 8.dp,
        startPadH = if (narrow) 8.dp else if (compact) 10.dp else 20.dp,
        startPadV = vPad,
        icon = if (narrow) 16.dp else if (compact) 18.dp else 20.dp,
        textSp = if (narrow) 13.sp else if (compact) 14.sp else 16.sp,
        gap = if (narrow) 6.dp else 8.dp,
        shadow = if (tier == RevealDockTier.STANDARD) 12.dp else 8.dp
    )
}

/**
 * Start exploring / Already … — a theme-aware inline row right below the
 * hero card. Reuses the tier metrics
 * so the pair resizes cleanly on every screen size.
 */
@Composable
private fun RevealActionRow(
    cat: CurioCategory,
    browseMode: Boolean,
    resolved: CurioTopic?,
    onExplore: () -> Unit,
    onAlready: () -> Unit,
    // v8.12 — browse mode: a non-tracking explore (no quests/passport/pet).
    onSilentExplore: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tier = when {
            maxWidth < 340.dp -> RevealDockTier.NARROW
            maxWidth < 440.dp -> RevealDockTier.COMPACT
            else -> RevealDockTier.STANDARD
        }
        // Inline under the hero, the width tiers (NARROW/COMPACT/STANDARD)
        // resize the pair cleanly on every screen.
        val m = revealDockMetrics(tier, tight = false)
        // v8.57 — the action row sits directly on the category wash, no
        // floating pill: transparent background so the buttons feel part
        // of the themed page instead of a disconnected surface card.
        Surface(
            shape = RoundedCornerShape(m.pillRadius),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = m.rowPadH,
                    vertical = m.rowPadV
                ),
                horizontalArrangement = Arrangement.spacedBy(m.rowGap),
                verticalAlignment = Alignment.CenterVertically
            ) {                    // Express yourself on the LEFT, Explore on the RIGHT.

                PetLandmark(
                    id = "express-yourself",
                    kind = PetLandmarks.Kind.FUN,
                    screen = "reveal"
                ) { lm ->
                RevealAlreadyButton(
                    // Express Yourself remains available from Topic Reveal,
                    // including the read-only reveal opened from Topic Database.
                    // During the pet-led tour the button is inert — the tour
                    // only TELLS you about it and advances via Next.
                    enabled = resolved != null && !TourController.active,
                    metrics = m,
                    modifier = lm.weight(1f),
                    onClick = onAlready
                )
                }
                if (!browseMode) {
                    // v8.22 — the Start exploring button is a tour landmark:
                    // the pet-guide highlights its REAL bounds on the tour step.
                    PetLandmark(
                        id = "start-exploring",
                        kind = PetLandmarks.Kind.FUN,
                        screen = "reveal"
                    ) { lm ->
                        RevealStartButton(
                            cat = cat,
                            // Inert during the pet-led tour — the action is
                            // only demonstrated, never started.
                            enabled = resolved != null && !TourController.active,
                            metrics = m,
                            modifier = lm.weight(1f),
                            onClick = onExplore
                        )
                    }
                } else if (onSilentExplore != null) {
                    // Browse mode: Explore opens the search page silently.
                    PetLandmark(
                        id = "start-exploring",
                        kind = PetLandmarks.Kind.FUN,
                        screen = "reveal"
                    ) { lm ->
                        RevealStartButton(
                            cat = cat,
                            // Inert during the pet-led tour — the action is
                            // only demonstrated, never started.
                            enabled = resolved != null && !TourController.active,
                            label = "Explore",
                            metrics = m,
                            modifier = lm.weight(1f),
                            onClick = onSilentExplore
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RevealStartButton(
    cat: CurioCategory,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    label: String = "Start exploring",
    metrics: RevealDockMetrics,
    onClick: () -> Unit
) {
    // v8.49 — filled CTA: the actions live on an OPAQUE floating pill (see
    // RevealActionDock), so this is a proper primary button instead of
    // transparent-on-wash. The pill carries the vertical room, so phones keep
    // a comfortable vertical padding — the old 2dp tight tier is gone.
    // v8.55 — the tier metrics resize the button instead of squeezing the
    // label on small screens.
    val startShape = RoundedCornerShape(50)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = startShape,
        colors = curioButtonColors(
            // v8.57 — themed to the category accent so the button wears the
            // lane's own color instead of the generic theme primary. v9.x —
            // AMOLED overrides to pitch-black (accent becomes the edge
            // shine); Material wears the device primary with the accent rim.
            containerColor = cat.themedButtonFill(),
            contentColor = cat.themedButtonInk(),
            disabledContainerColor = cat.themedButtonFill().copy(alpha = 0.35f),
            disabledContentColor = cat.themedButtonInk().copy(alpha = 0.45f)
        ),
        contentPadding = PaddingValues(
            horizontal = metrics.startPadH,
            vertical = metrics.startPadV
        ),
        modifier = modifier
            .fillMaxWidth()
            .categoryEdgeShine(startShape, accent = cat.themedAccent())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(metrics.gap)
        ) {
            CurioIcon(
                CurioIcons.AutoAwesome,
                null,
                tint = cat.onAccent(),
                size = metrics.icon
            )
            Text(
                text = label,
                // Tier-scaled type: 13sp on ~320dp screens, 14sp on phones,
                // 16sp on tablets — "Start exploring" stays on one line
                // instead of ellipsizing (v8.44 review, v8.55 tiers).
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = metrics.textSp,
                    fontWeight = FontWeight.ExtraBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RevealAlreadyButton(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    metrics: RevealDockMetrics,
    onClick: () -> Unit
) {
    // v8.49 — text-style writing action on the transparent inline row.
    // v8.55 — the tier metrics resize it with the pill.
    val baseInk = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = if (enabled) baseInk else baseInk.copy(alpha = 0.40f)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = modifier
            // Give the writing action a real, forgiving tap target across its
            // entire weighted half of the row. The old inner padding made the
            // visible label look wider than the actual touchable surface.
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CurioIcon(
                name = CurioIcons.Edit,
                contentDescription = null,
                tint = ink,
                size = metrics.icon
            )
            Spacer(Modifier.width(metrics.gap))
            Text(
                text = "Express yourself",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = metrics.textSp,
                    fontWeight = FontWeight.Bold
                ),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Hero card — large category watermark with verb + duration badge
// ═══════════════════════════════════════════════════════════════════════════

/** The reveal hero's resting height (matches the pre-v8.36 fixed size). */
private val RevealHeroBaseHeight = 260.dp

@Composable
private fun HeroCard(
    cat: com.curio.app.data.CurioCategory,
    resolved: CurioTopic?,
    modifier: Modifier = Modifier
) {
    // ── Shared-element handoff (Topic Reveal morph) ──────────────────────
    // Matches the Spin front ticket's "reveal-hero" element: when this
    // topic is opened from the deck, the hero expands out of the card's
    // position (no match — e.g. opened from Home/Recent — renders in
    // place, no animation). Provided by the NavHost via composition locals.
    val sharedTransitionScope = LocalRevealSharedScope.current ?: return
    val animatedVisibilityScope = LocalRevealVisibilityScope.current ?: return
    val revealSharedState = sharedTransitionScope.rememberSharedContentState(RevealSharedElementKey)

    val action = resolved?.exploreAction
    val accent = cat.themedAccent()
    val heroGradient = CurioGradients.cardGradient(accent)
    // v7.5 — pastel mode lightens the hero gradient, so the pill content
    // flips from white to the deep accent (light) / light twin (dark).
    // Match the Spin ticket's ink formula exactly so the morph reads as
    // the same card expanding: pastel → pastelFillInk, else → onAccent.
    val ink = if (AppPreferences.pastelColorsState) pastelFillInk(accent) else cat.onAccent()

    // ── Gradient brush — match the Spin ticket's formula so the card
    //    reads as the same surface during the morph. When heroGradientOn
    //    is enabled, the hero gets the same top-lit diagonal sweep as the
    //    deck's front ticket; otherwise a plain vertical gradient.
    val dark = isCurioDarkTheme()
    val pastelLightHero = AppPreferences.pastelColorsState && !dark
    val heroGradientOn = AppPreferences.heroGradientState
    val heroBorderOn = AppPreferences.heroBorderState

    // v8.36 — auto-growing hero: the title used to be hard-capped at 3
    // lines, cutting very long topic names. The card now measures how much
    // of the title spills past the 3-line fold (in px, from the layout
    // result — so the growth tracks the REAL line height, including
    // accessibility font scaling) and grows past the 260dp base by exactly
    // that amount, so long titles show in full. The measurement is latched
    // only after it has been stable for 420ms — during the shared-element
    // morph the card resizes every frame and the title reflows, so latching
    // mid-morph would make the height fight the expansion. The height
    // change itself is animated so the grow reads as a gentle bloom, not a
    // snap.
    // Float state: the overflow is measured in px (size.height minus the
    // 3rd line's bottom — both floats; Int minus Float yields Float), so
    // Int state here was a compile error (v8.37 CI fix).
    var titleOverflowPx by remember(resolved) { mutableStateOf(0f) }
    var settledOverflowPx by remember(resolved) { mutableStateOf(0f) }
    LaunchedEffect(titleOverflowPx) {
        delay(420)
        settledOverflowPx = titleOverflowPx
    }
    val density = LocalDensity.current
    val heroHeight by animateDpAsState(
        targetValue = RevealHeroBaseHeight +
            with(density) { settledOverflowPx.toDp() },
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "revealHeroHeight"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
            .then(
                // Shared-element target: bounds animate from the Spin
                // ticket's position/size to this hero when the topic opens.
                sharedTransitionScope.run {
                    Modifier.sharedElement(
                        revealSharedState,
                        animatedVisibilityScope,
                        boundsTransform = RevealBoundsTransform
                    )
                }
            ),
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(30.dp))
                .then(
                    // Hero border — whisper hairline matching the ticket
                    if (heroBorderOn) {
                        Modifier.drawBehind {
                            val borderW = 1.5.dp.toPx()
                            val radius = 30.dp.toPx() - borderW / 2f
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        lerp(ink, Color.White, if (dark) 0.20f else 0.30f),
                                        lerp(ink, accent, 0.14f)
                                    )
                                ),
                                topLeft = Offset(borderW / 2f, borderW / 2f),
                                size = Size(size.width - borderW, size.height - borderW),
                                cornerRadius = CornerRadius(radius, radius),
                                style = Stroke(width = borderW)
                            )
                        }
                    } else Modifier
                )
        ) {
            // ── Gradient brush — pixel-perfect match with the Spin ticket:
            //    same color stops AND the same diagonal linearGradient when
            //    heroGradientOn is enabled, so every pixel reads identical
            //    during the morph.
            val density = LocalDensity.current
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val heroBrush = if (heroGradientOn) {
                val crown = lerp(heroGradient.first(), Color.White, if (pastelLightHero) 0.08f else 0.16f)
                val base = lerp(heroGradient.last(), Color.Black, 0.06f)
                val stops = if (heroGradient.size > 2) {
                    listOf(crown) + heroGradient.drop(1).dropLast(1) + listOf(base)
                } else {
                    CurioGradients.hslGradientStops(crown, base, 3)
                }
                Brush.linearGradient(stops, start = Offset(0f, 0f), end = Offset(wPx, hPx))
            } else {
                Brush.verticalGradient(heroGradient)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(heroBrush, RoundedCornerShape(30.dp))
            ) {
            // ── Watermark glyph (category icon) — matches the Spin ─────
            //    ticket's exact glyph: same size (150dp), same position
            //    (CenterEnd + 6dp end), same tint (ink at 0.16 alpha), so
            //    the morph reads as the same card expanding.
            CurioIcon(
                name = cat.iconGlyph,
                contentDescription = null,
                tint = ink.copy(alpha = 0.16f),
                size = 150.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
            )
            // ── Content column — the topic NAME lives INSIDE the hero so
            //    the shared-element morph grows it with the card (v8.25):
            //    opening a landed topic no longer pops a separate headline
            //    in below the card — the title expands in place with the
            //    gradient, watermark and pills, so it reads as staying put
            //    through the whole morph. Same 34sp/38sp geom style and
            //    left alignment as the Spin ticket's title for a 1:1
            //    handoff; the card's ink keeps it legible on the gradient.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // ── Top — action badge (verb + duration) ────────────────
                if (action != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ink.copy(alpha = 0.18f),
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ink)
                            )
                            Text(
                                text = "${action.verb} for ~${action.durationMinutes} min",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ink
                            )
                        }
                    }
                }

                // ── Middle — the topic name (auto-grows the card) ──────
                // Weighted spacers (not SpaceBetween) keep the title centred
                // between the badge row and the bottom pills whether or not
                // the badge is present (e.g. while the topic loads). The
                // measured line count feeds the hero's height so long names
                // wrap in full instead of being cut at 3 lines (v8.36).
                Spacer(Modifier.weight(1f))
                Text(
                    text = resolved?.name ?: cat.displayName,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 34.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = ink,
                    // Height of the lines past the 3-line fold (0 when the
                    // title fits) — feeds the auto-growing hero above.
                    onTextLayout = { result ->
                        titleOverflowPx = if (result.lineCount > 3) {
                            result.size.height - result.getLineBottom(2)
                        } else 0f
                    }
                )
                Spacer(Modifier.weight(1f))

                // ── Bottom — byline + subtype pills, one per corner ─────
                val byline = resolved?.byline?.takeIf { it.isNotBlank() }
                val bylineLabel = when (cat.id) {
                    CategoryId.ALBUMS -> "Artist"
                    CategoryId.BOOKS -> "Author"
                    CategoryId.FILMS -> "Director"
                    CategoryId.ARTWORKS -> "Painter"
                    CategoryId.DISCOVERIES -> "Discovered by"
                    else -> null
                }
                val subtype = resolved?.subtype?.takeIf { it.isNotBlank() }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left slot — byline pill (or a blank spacer so a lone
                    // subtype still pins to the right corner). weight(1f,
                    // fill = false) bounds the pill to the space left after
                    // the subtype, so a long byline ellipsizes instead of
                    // overflowing the row on narrow screens.
                    if (byline != null && bylineLabel != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ink.copy(alpha = 0.18f),
                            shadowElevation = 0.dp,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CurioIcon(
                                    name = CurioIcons.Person,
                                    contentDescription = null,
                                    tint = ink,
                                    size = 14.dp
                                )
                                Text(
                                    text = "$bylineLabel · $byline",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier)
                    }
                    // Right slot — subtype pill (blank spacer keeps the
                    // byline on its left corner when there's no subtype).
                    if (subtype != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ink.copy(alpha = 0.18f),
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = subtype,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = ink,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier)
                    }
                }
            }
            } // inner background Box
        } // BoxWithConstraints
    } // HeroCard Surface
}

// ═══════════════════════════════════════════════════════════════════════════
// Teaser card ("One quirky fact to get you curious")
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun TeaserCard(
    cat: com.curio.app.data.CurioCategory,
    teaser: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cat.categorySurface(MaterialTheme.colorScheme.surface),
        shadowElevation = 0.dp,
        border = cat.categoryBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurioIcon(
                    name = CurioIcons.AutoAwesome,
                    contentDescription = null,
                    tint = cat.categoryInk(),
                    size = 16.dp
                )
                Text(
                    text = "One quirky fact to get you curious",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = teaser ?: "Loading topic…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═════════════════���═════════════════════════════════════════════════════════
// Action prompt card ("{verb} {target}" + instruction)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ActionPromptCard(
    cat: com.curio.app.data.CurioCategory,
    action: com.curio.app.data.ExploreAction,
    subtype: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow),
        shadowElevation = 0.dp,
        border = cat.categoryBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    CurioIcon(
                        name = verbIcon(action.verb),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 18.dp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${action.verb} ${action.targetName}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtype.isNotBlank()) {
                        Text(
                            text = subtype,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = action.instruction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** One-shot soft entrance for the reveal content below the morphing hero —
 *  fades + rises gently so the page blooms in after the card expands, with
 *  a light stagger (later sections delay a touch more). */
@Composable
private fun RevealContentEntrance(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(320, delayMillis = delayMillis, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            animationSpec = tween(320, delayMillis = delayMillis, easing = FastOutSlowInEasing)
        ) { height -> height / 28 }
    ) { content() }
}

/** Map exploreAction verb to a Material Symbols glyph (no emoji). */
private fun verbIcon(verb: String): String = when (verb.lowercase().trim()) {
    "listen" -> "headphones"
    "watch" -> "play_arrow"
    "read" -> "menu_book"
    "look at", "look", "view" -> "image"
    "explore" -> "explore"
    "read about", "think about" -> "auto_awesome"
    "research" -> "search"
    "cook" -> "restaurant"
    "build" -> "construction"
    "write" -> "edit"
    "play" -> "play_arrow"
    else -> "auto_awesome"
}

/** The explore-dialog copy for what actually opens — mirrors
 *  categoryOpensYouTube so the copy can never drift from the URL built by
 *  buildExploreSearchUrl. */
private fun exploreOpenCopy(cat: com.curio.app.data.CurioCategory): String =
    if (categoryOpensYouTube(cat.id)) "We'll open YouTube to get you started."
    else "We'll open a Google search to get you started."


/** Circular like/dislike toggle — active state fills with the category accent. */
@Composable
private fun SentimentButton(
    icon: String,
    label: String,
    active: Boolean,
    accent: Color,
    ink: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) accent else MaterialTheme.colorScheme.surfaceVariant,
        border = if (active) null
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurioIcon(
                name = icon,
                contentDescription = label,
                tint = if (active) ink else MaterialTheme.colorScheme.onSurface,
                size = 18.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (active) ink else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** POST_NOTIFICATIONS is a no-op below API 33 — treated as granted. */
private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
