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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioPassport
import com.curio.app.data.CurioTopic
import com.curio.app.data.ExploreReminderScheduler
import com.curio.app.data.ExploreSession
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.TopicCatalog
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.buildExploreSearchUrl
import com.curio.app.data.categoryOpensYouTube
import com.curio.app.infrastructure.ExploreSessionService
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.LocalRevealSharedScope
import com.curio.app.ui.adaptive.LocalRevealVisibilityScope
import com.curio.app.ui.adaptive.RevealBoundsTransform
import com.curio.app.ui.adaptive.RevealSharedElementKey
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioWatermarkBackdrop
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
 *   ~260 dp    Hero card (gradient ticket: watermark glyph + badges)
 *   24 dp      gap
 *   ~84 dp     Topic name (geom displaySmall, multi-line)
 *    8 dp      gap
 *   ~42 dp     Tags chip row
 *   20 dp      gap
 *   ~auto     "One quirky fact to get you curious" card
 *   16 dp      gap
 *   ~auto     "{verb} {target}" action prompt card + "~N min"
 *   24 dp      content breathing room
 *   bottom     themed action dock (Start exploring + Already …)
 *   navigation-bar inset is applied inside the dock
 */
@Composable
fun TopicRevealScreen(
    categorySlug: String,
    topicName: String,
    navController: NavController,
    // Browse-Topics read-only mode (see CurioRoutes.REVEAL): no explore
    // CTA, no like/dislike, no recents recording, and "Already watched"
    // confirms without the write-about-it dialog.
    browseMode: Boolean = false,
    onBottomBarContentChanged: (@Composable () -> Unit) -> Unit = {},
    onBottomBarContentCleared: () -> Unit = {}
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
    val isDone = resolved != null && ExploreSessionStore.isDone(cat.id, resolved.name)
    // v7 — like/dislike teaches the shuffle: liked topics (and their whole
    // category) get more weight, disliked get less — never fully blocked.
    // Reads the REACTIVE sentiment state so the buttons toggle instantly.
    val sentiment = resolved?.let { AppPreferences.topicSentiment(cat.id, it.id) }

    // v8.5 — Category passport: opening a reveal counts as a "peek" for the
    // lane's stamp and refreshes its last-explored date (spec §6.1). Fires
    // once per topic opened; a rotation re-fire is harmless (the stamp
    // derives from counts > 0, never exact values).
    LaunchedEffect(cat.id, resolved?.id) {
        if (resolved != null) CurioPassport.noteReveal(context, cat.id)
    }

    // Explore-session flow — tapping the CTA records the topic as
    // recently-explored the moment it's tapped (even before anything is
    // saved to the Cabinet), then opens a two-way dialog (Explore now /
    // Write about it). Leaving the screen without engaging records it as
    // recently-unexplored so Home can offer to resume it.
    var engaged by rememberSaveable { mutableStateOf(false) }
    var showExploreDialog by rememberSaveable { mutableStateOf(false) }
    // v7.80 — "Already watched/listened/read/explored" — marks the topic
    // done (never shows in the shuffle again) and asks whether to write
    // about it now.
    var showAlreadyDoneDialog by rememberSaveable { mutableStateOf(false) }

    val latestBrowseMode by rememberUpdatedState(browseMode)
    val latestResolved by rememberUpdatedState(resolved)
    val latestIsDone by rememberUpdatedState(isDone)
    val latestOnExplore by rememberUpdatedState<() -> Unit> { showExploreDialog = true }
    val latestOnAlready by rememberUpdatedState<() -> Unit> {
        val currentTopic = latestResolved
        if (currentTopic != null) {
            engaged = true
            if (latestIsDone) {
                ExploreSessionStore.unmarkDone(context, cat.id, currentTopic.name)
            } else {
                ExploreSessionStore.markDone(context, cat.id, currentTopic.name)
                if (!latestBrowseMode) showAlreadyDoneDialog = true
            }
        }
    }
    val revealBottomBar = remember(cat) {
        @Composable {
            RevealActionDock(
                cat = cat,
                browseMode = latestBrowseMode,
                resolved = latestResolved,
                isDone = latestIsDone,
                onExplore = latestOnExplore,
                onAlready = latestOnAlready
            )
        }
    }
    DisposableEffect(revealBottomBar) {
        onBottomBarContentChanged(revealBottomBar)
        onDispose { onBottomBarContentCleared() }
    }

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

    // ── Floating explore bubble permission ────────────────────────────
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
    fun startExploreSession(topic: CurioTopic) {
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
            searchUrl = buildExploreSearchUrl(topic),
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

                // ── 3. Topic name ───────────────────────────────────────────
                // The headline blooms in with a soft fade + rise right after
                // the hero morph. The hero is now the ONLY shared element:
                // the old shared-element title morph stretched the text (a
                // 34sp card title scaled up to a full-width 40sp headline)
                // and could land at the wrong spot on back — text can't be
                // re-wrapped smoothly in SharedTransitionScope, so the name
                // gets a clean entrance instead. Secondary entry points
                // (Home/Recents/Browse) get the same treatment.
                RevealContentEntrance(delayMillis = 80) {
                    Text(
                        text = resolved?.name ?: cat.displayName,
                        style = MaterialTheme.typography.displaySmall.copy(
                            lineHeight = 40.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                    )
                }

                // ── 4. Tags chip row (genre / era context) ─────────────────
                RevealContentEntrance(delayMillis = 110) {
                    if (!resolved?.tags.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            resolved.tags.take(4).forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = cat.themedAccent().copy(alpha = 0.18f),
                                    shadowElevation = 0.dp
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Spacer so subsequent sections don't crowd up.
                        Spacer(Modifier.height(10.dp))
                    }
                }

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

                // The action dock is rendered by the Scaffold bottom slot so
                // it occupies the existing reveal placeholder below this
                // scrolling content.
                Spacer(Modifier.height(24.dp))
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
                        "other apps — even while you're in the browser. It needs " +
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

    // v7.80/v7.92 — "Already …" confirmation: shown right after marking the
    // topic done; it only asks whether to write about it now. Dismissing
    // closes the dialog and STAYS on the reveal screen — the button has
    // already flipped to the marked-done state (tapping it again undoes),
    // so there's no reason to pop back anymore.
    if (showAlreadyDoneDialog && resolved != null) {
        val topic = resolved
        AlertDialog(
            onDismissRequest = { showAlreadyDoneDialog = false },
            title = { Text("${alreadyDoneLabel(cat)}?") },
            text = {
                Text(
                    "${topic.name} is marked as done and won't show up in the " +
                        "next shuffle. Want to write about it now?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAlreadyDoneDialog = false
                    navController.navigate(CurioRoutes.captureFor(cat.id.routeSlug, topic.name)) {
                        launchSingleTop = true
                    }
                }) { Text("Write about it") }
            },
            dismissButton = {
                TextButton(onClick = { showAlreadyDoneDialog = false }) { Text("Not now") }
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
                        "Time to ${action.verb.lowercase()} ${action.targetName} — roughly ${action.durationMinutes} min. ${exploreOpenCopy(cat)}",
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
                TextButton(onClick = { startExploreSession(topic) }) { Text("Explore now") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        engaged = true
                        // Writing about it counts as engaging — record as
                        // recently-explored (clears the unexplored entry,
                        // tagging "Resumed" if the user came back to it).
                        ExploreSessionStore.recordExplored(context, cat.id, topic.name)
                        ExploreSessionStore.removeUnexplored(context, cat.id, topic.name)
                        showExploreDialog = false
                        navController.navigate(CurioRoutes.captureFor(cat.id.routeSlug, topic.name)) {
                            launchSingleTop = true
                        }
                    }
                ) { Text("Write about it") }
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
                            "The current session gets queued — paused with its time banked, " +
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

@Composable
private fun RevealActionDock(
    cat: com.curio.app.data.CurioCategory,
    browseMode: Boolean,
    resolved: CurioTopic?,
    isDone: Boolean,
    onExplore: () -> Unit,
    onAlready: () -> Unit
) {
    // Wash-backed dock: the dock wears the SAME category wash the reveal
    // page paints behind it, so the two (fully transparent) actions float
    // on the page tint exactly like a transparent dock — but unlike true
    // transparency, the wash also covers the nav-bar strip below the
    // buttons, so the Scaffold's cream background can never show through as
    // a band behind the dock. (v8.5 — the Scaffold containerColor must stay
    // constant across the route transition; the wash lives HERE instead.)
    // v8.5 regression fix (THE morph freeze): the dock must reserve EXACTLY
    // the same total height as the bottom bar it replaces (80dp — M3's
    // NavigationBar consumes the nav-bar inset inside its 80dp min height,
    // and the NavHost's morph placeholder reserves that measured height). A
    // taller dock (80dp + inset via heightIn) changed Scaffold innerPadding
    // the moment this dock swapped in for the placeholder mid-transition,
    // re-laying out the SharedTransitionLayout and freezing the shared-
    // element morph — the whole reveal page stayed invisible except this
    // dock, which lives outside the layout. The nav-bar inset is still
    // consumed, but INSIDE the fixed 80dp (height first, then the inset as
    // internal padding), so the buttons clear the gesture bar without
    // growing the dock past the reserved slot.
    Surface(
        color = cat.categoryBackgroundWash(),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Already/Undo on the LEFT, Start exploring on the RIGHT.
            RevealAlreadyButton(
                enabled = resolved != null,
                cat = cat,
                isDone = isDone,
                modifier = Modifier.weight(1f),
                onClick = onAlready
            )
            if (!browseMode) {
                RevealStartButton(
                    enabled = resolved != null,
                    cat = cat,
                    modifier = Modifier.weight(1f),
                    onClick = onExplore
                )
            }
        }
    }
}

@Composable
private fun RevealStartButton(
    enabled: Boolean,
    cat: com.curio.app.data.CurioCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Fully transparent: the accent text + icon float directly on the page
    // wash (categoryInk is theme-aware — deep accent in light, light twin
    // in dark). Disabled fades the ink instead of showing a tinted fill.
    val ink = cat.categoryInk()
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = ink,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = ink.copy(alpha = 0.40f)
        ),
        // Compact vertical padding: the dock is a fixed 80dp with the nav-bar
        // inset consumed inside, so on inset-heavy devices the content area is
        // ~50dp — the buttons must fit without clipping the gesture bar.
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CurioIcon(CurioIcons.AutoAwesome, null, tint = ink, size = 20.dp)
            Text(
                text = "Start exploring",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
        }
    }
}

@Composable
private fun RevealAlreadyButton(
    enabled: Boolean,
    cat: com.curio.app.data.CurioCategory,
    isDone: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Fully transparent: the label + icon float on the page wash. Done
    // state reads in the category ink (mirrors the Start button); idle
    // state in the muted onSurfaceVariant. Disabled fades the ink to match
    // the Start button's disabled treatment while the topic loads.
    val baseInk = if (isDone) cat.categoryInk() else MaterialTheme.colorScheme.onSurfaceVariant
    val ink = if (enabled) baseInk else baseInk.copy(alpha = 0.40f)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            // Compact vertical padding — see RevealStartButton's comment.
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            CurioIcon(
                name = if (isDone) CurioIcons.Close else CurioIcons.History,
                contentDescription = null,
                tint = ink,
                size = 18.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isDone) undoLabel(cat) else alreadyDoneLabel(cat),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = ink
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Hero card — large category watermark with verb + duration badge
// ═══════════════════════════════════════════════════════════════════════════

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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
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
            // ── Action badge (verb + duration) — white pill on gradient ───
            if (action != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ink.copy(alpha = 0.18f),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
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
            // ── Byline pill (artist / author / director / painter /
            //    discoverer) — "Artist · The Beatles" — mirrors the subtype
            //    pill on the opposite corner so the work's creator reads at
            //    a glance.
            val byline = resolved?.byline?.takeIf { it.isNotBlank() }
            val bylineLabel = when (cat.id) {
                CategoryId.ALBUMS -> "Artist"
                CategoryId.BOOKS -> "Author"
                CategoryId.FILMS -> "Director"
                CategoryId.ARTWORKS -> "Painter"
                CategoryId.DISCOVERIES -> "Discovered by"
                else -> null
            }
            if (byline != null && bylineLabel != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ink.copy(alpha = 0.18f),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
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
            }
            // ── Subtype pill ────────────────────
            if (resolved?.subtype?.isNotBlank() == true) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ink.copy(alpha = 0.18f),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Text(
                        text = resolved.subtype,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ink,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
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

/** The "Already …" label — the category's verb, per category (v7.80).
 *  Films/Directors → watched, Albums/Artists → listened, Books/Authors →
 *  read, Artworks/Painters → seen, everything else → explored. */
private fun alreadyDoneLabel(cat: com.curio.app.data.CurioCategory): String = when (cat.id) {
    CategoryId.FILMS, CategoryId.DIRECTORS -> "Already watched"
    CategoryId.ALBUMS, CategoryId.ARTISTS -> "Already listened"
    CategoryId.BOOKS, CategoryId.AUTHORS -> "Already read"
    CategoryId.ARTWORKS -> "Already seen"
    CategoryId.PAINTERS -> "Already explored"
    else -> "Already explored"
}

/** The undo label — mirrors [alreadyDoneLabel] per category (Films →
 *  "Unwatched" ↔ "Already watched", Books → "Unread" ↔ "Already read",
 *  etc.). Shown on the already-pill's done state; tapping unmarks it. */
private fun undoLabel(cat: com.curio.app.data.CurioCategory): String = when (cat.id) {
    CategoryId.FILMS, CategoryId.DIRECTORS -> "Unwatched"
    CategoryId.ALBUMS, CategoryId.ARTISTS -> "Unlistened"
    CategoryId.BOOKS, CategoryId.AUTHORS -> "Unread"
    CategoryId.ARTWORKS -> "Unseen"
    CategoryId.PAINTERS -> "Unexplored"
    else -> "Unexplored"
}

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
