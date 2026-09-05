package com.curio.app.features.reveal

import android.Manifest
import android.app.Activity
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
// Note: EnterTransition/ExitTransition combine with + (the operator resolves
// without an explicit import on this Compose BOM — see other UI files).
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.curio.app.R
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioPassport
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.data.CurioCategory
import com.curio.app.data.AlbumTrack
import com.curio.app.data.BookChapter
import com.curio.app.data.CurioTopic
import com.curio.app.data.ExploreReminderScheduler
import com.curio.app.data.ExploreSession
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.TourController
import com.curio.app.data.MusicService
import com.curio.app.data.TopicCatalog
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.TopicRepository
import com.curio.app.data.buildEngineSearchUrl
import com.curio.app.data.buildExploreQuery
import com.curio.app.data.buildExploreSearchUrl
import com.curio.app.data.buildMusicServiceSearchUrl
import com.curio.app.data.buildYouTubeSearchUrl
import com.curio.app.data.derivedDecadeTag
import com.curio.app.data.isMusicTopic
import com.curio.app.data.titleAndYearQualifier
import com.curio.app.data.matchesSavedName
import com.curio.app.data.matchesSavedNameStrict
import com.curio.app.data.CoverSwatches
import com.curio.app.data.fetchCoverSwatches
import com.curio.app.data.openSearchUrl
import com.curio.app.data.resolveAppleMusicItemUrl
import com.curio.app.data.resolveSpotifyItemUrl
import com.curio.app.infrastructure.ExploreSessionService
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.SentimentPillHost
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.LocalRevealSharedScope
import com.curio.app.ui.adaptive.LocalRevealVisibilityScope
import com.curio.app.ui.adaptive.RevealBoundsTransform
import com.curio.app.ui.adaptive.RevealSharedElementKey
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.features.settings.BookCoverFetch
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioProgressPill
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.curioFloatingNavContainerFor
import com.curio.app.ui.components.isLiquidGlassRequested
import com.curio.app.ui.components.liquidGlassCapsule
import com.curio.app.ui.components.categoryEdgeShine
import com.curio.app.ui.components.curioButtonColors
import com.curio.app.ui.components.curioDarkGlow
import com.curio.app.ui.components.curioGlassEdge
import com.curio.app.ui.components.curioInnerGlow
import com.curio.app.ui.theme.ChangaOneFontFamily
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioEditorialBody
import com.curio.app.ui.theme.CurioGradients
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.brandRes
import com.curio.app.ui.theme.categoryBackgroundWash
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.notesSheetContainerColor
import com.curio.app.ui.theme.notesSheetPalette
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.lightAccentTint
import com.curio.app.ui.theme.oklabGradientStops
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.themedAccent
import com.curio.app.ui.theme.toHsl
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
 *  - Tags chips directly below the hero — instant context for genres /
 *    eras (e.g. "1970s · British · Art Rock") without cluttering the body.
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
 *   ~auto     Tags chips (directly below the hero)
 *   ~auto     Inline actions (Start exploring + Already …)
 *   ~auto     "One quirky fact to get you curious" card
 *   16 dp      gap
 *   ~auto     "{verb} {target}" action prompt card + "~N min"
 *   100 dp     bottom clearance for the floating Like/Dislike pill
 *   floating  Like/Dislike capsule — slides away while scrolling down,
 *             back in on scroll-up (v132)
 */

/** v49 — ONE editorial paragraph voice for the reveal's long-form copy: the
 *  quick fact and the action instruction share this exact style — matched
 *  size/leading, a notch below the original 17sp fact (15sp) so the page
 *  reads lighter and the pair can never drift apart again. */
private val RevealEditorialBody: TextStyle = CurioEditorialBody.copy(
    fontSize = 15.sp,
    lineHeight = 23.sp
)

/**
 * v316 — instant topic resolution for the reveal, purely from memory:
 * the warm lane cache first, then the prewarmed MERGED INDEX (which also
 * survives lane-cache memory trims and carries wildcard.json originals).
 * Synchronous and parse-free — so any topic that has ever been loaded
 * resolves on the very first composition frame. (Deliberately NOT
 * @Composable: it runs inside remember {}'s calculation lambda.)
 */
private fun resolveRevealTopic(categoryId: CategoryId, topicName: String): CurioTopic? {
    TopicJsonLoader.cached(categoryId)?.firstOrNull {
        it.matchesSavedNameStrict(topicName) || it.matchesSavedName(topicName)
    }?.let { return it }
    TopicJsonLoader.cachedIndex()?.firstOrNull { entry ->
        entry.topic.categoryId == categoryId &&
            (entry.topic.matchesSavedNameStrict(topicName) || entry.topic.matchesSavedName(topicName))
    }?.let { return it.topic }
    return null
}

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

    // Room is the source of truth. Resolve by category first, then use the
    // warm loader only as a compatibility fallback while an older install is
    // finishing its one-time import. Never use the global catalog here: a
    // duplicate title in another category must not open the wrong topic.
    val context = LocalContext.current
    // Satisfying haptics resolved in composition (never inside the click
    // lambdas) — firm confirm for saves, light ticks for toggles/actions.
    val haptics = LocalHapticFeedback.current
    // v315/v316 — resolve from the ALREADY-WARM in-memory caches on the very
    // first frame (synchronous, zero parses): MainActivity prewarms every
    // lane from Room at app start, any topic opened before stays in the
    // loader cache, and the prewarmed merged index covers even topics whose
    // lane cache was shed or wildcard.json originals. So ANY topic that has
    // ever been loaded shows its quick fact + metadata immediately instead
    // of waiting on async Room/JSON resolution — no 1-second blank flash
    // for topics opened for the first time either.
    var resolved by remember(topicName, cat.id) {
        mutableStateOf(resolveRevealTopic(cat.id, topicName))
    }
    var showSynopsisDialog by rememberSaveable { mutableStateOf(false) }
    var selectedChapter by remember { mutableStateOf<BookChapter?>(null) }
    var showAlbumSheet by rememberSaveable { mutableStateOf(false) }
    var selectedAlbumTrack by remember { mutableStateOf<AlbumTrack?>(null) }
    // v350 — the series episode-list sheet (album-style) for SERIES topics.
    var showSeriesSheet by rememberSaveable { mutableStateOf(false) }
    // v315 — the book/album sections compose only AFTER the shared-element
    // morph settles (~380ms), so heavy content (poster Coil decode, chapter
    // LazyRow, album track list) never competes with the card expansion
    // frames — the morph stays smooth on topics with synopsis + chapters +
    // album track lists.
    var contentUiReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(380); contentUiReady = true }
    LaunchedEffect(topicName, cat.id) {
        // Init is a no-op once Room is populated; skip the mutex wait entirely
        // on warm starts so the resolution starts immediately.
        if (!TopicRepository.isInitialized()) TopicRepository.init(context)
        resolved = TopicRepository.findTopic(context, cat.id, topicName)
            // v316 — the merged index may have finished building between the
            // composition seed and this effect; it answers without any parse.
            ?: resolveRevealTopic(cat.id, topicName)
            ?: runCatching {
                // Room may be missing this topic: the one-time import can
                // still be running, or the data gained topics between app
                // updates (the version-gated sync hasn't re-run) — and a
                // stale-but-populated Room lane would mask TopicJsonLoader's
                // fast path. Parse the lane's JSON DIRECTLY (bypassing the
                // Room mask) and REPLACE-upsert it, so the reveal NEVER
                // opens blank and the topic is in Room from then on. For
                // WILDCARD topics this merges every lane + wildcard.json
                // (the wildcard pool isn't stored under its own Room
                // category), so it keeps the shared loader path.
                val pool = if (cat.id == CategoryId.WILDCARD)
                    TopicJsonLoader.load(cat.id)
                else
                    TopicRepository.refreshLaneFromAssets(context, cat.id)
                        ?: TopicJsonLoader.load(cat.id)
                pool.firstOrNull { it.matchesSavedNameStrict(topicName) }
                    ?: pool.firstOrNull { it.matchesSavedName(topicName) }
            }.getOrNull()
            ?: runCatching {
                // Last resort (saved wildcard curiosities / renamed topics):
                // exhaustive search across every lane.
                TopicCatalog.findByNameAcrossAll(topicName)
            }.getOrNull()
        // Keep explored topics durable: persist the resolved topic into the
        // cached_topics table so it survives even a catalog-table wipe and is
        // never parsed again on later visits.
        resolved?.let { TopicRepository.rememberTopic(context, it) }
    }

    // v29 — clipboard for the auto-copy on explore: the search query lands on
    // the clipboard so the user can paste it into an app's own search box
    // (Spotify / Apple Music don't always hand off an in-app search).
    val clipboard = LocalClipboardManager.current
    // v6.7 — pin for later: the bookmark toggles on/off so the user can save
    // the topic and revisit it from Topic History → "Pinned for later".
    // Reads the REACTIVE pinnedTopicsState (not prefs) so the icon toggles
    // immediately when the user taps pin/unpin.
    val isPinned = resolved != null &&
        AppPreferences.pinnedTopicsState.any { it.categoryId == cat.id && it.topicName == resolved?.name }
    // v7.92 — reactive done state: the "Already …" button flips to a filled
    // marked state once the topic is done, and the unwatch action appears.
    // Reads doneTopicsState inside composition, so it updates the moment
    // markDone / unmarkDone changes the set.
    // v7 — like/dislike teaches the shuffle: liked topics (and their whole
    // category) get more weight, disliked get less — never fully blocked.
    // Reads the REACTIVE sentiment state so the buttons toggle instantly.
    val sentiment = resolved?.let { AppPreferences.topicSentiment(cat.id, it.id) }
    // v52b — async Apple Music item lookup (the reveal's Watch-in tap).
    val revealScope = rememberCoroutineScope()

    // v8.5 — Category passport: opening a reveal counts as a "peek" for the
    // lane's stamp and refreshes its last-explored date (spec §6.1). Fires
    // once per topic opened; a rotation re-fire is harmless (the stamp
    // derives from counts > 0, never exact values).
    // v8.6 — the First Journey tour's "Open the landed topic" step advances
    // the moment the reveal really opens (spec §7.2 step 5).
    LaunchedEffect(cat.id, resolved?.id, browseMode) {
        if (resolved != null && !browseMode) {
            CurioPassport.noteReveal(context, cat.id)
            // v9.x — reveal dailies ("Reveal 2 new topics" etc.) feed here.
            CurioQuests.noteReveal(context)
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
    // v22 — explore-bubble opt-in inside the explore dialog. Defaults to the
    // Settings pref (OFF for fresh installs); the choice is applied to prefs
    // when an action is picked, so flipping it here is the same as the
    // Settings toggle (persistent - last choice wins).
    var bubbleOptIn by rememberSaveable {
        mutableStateOf(AppPreferences.isOverlayBubbleEnabled(context))
    }

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
    // v25 — browse-mode Explore (opened from the Topic Database) now runs the
    // REAL explore session — same dialog, timer, recents and done-mark as the
    // Spin deck — instead of the old silent out-of-app search (v8.12). The
    // user asked why Explore didn't start a session from the browser.
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
    // v229 — POST_NOTIFICATIONS permanently-denied checker dialog + the
    // settings-return flag for its ON_RESUME continuation. Declared BEFORE
    // the permission launcher below: Kotlin locals aren't visible to a
    // lambda that appears earlier in the function (the CI compile error).
    var showNotificationsBlockedDialog by rememberSaveable { mutableStateOf(false) }
    var awaitingNotificationsSettings by remember { mutableStateOf(false) }

    /** Opens the search page (the chosen search engine — YouTube for music),
     *  then lands back on Home — returning to the
     *  app triggers the "are you done exploring?" prompt. Deferred into the
     *  permission callback when a notification-permission request is in
     *  flight, so the foreground service starts while this activity is still
     *  foreground (a background FGS start throws on Android 12+). */
    fun openExploreBrowserAndGoHome(session: ExploreSession) {
        showExploreDialog = false
        openSearchUrl(context, session.searchUrl)
        navController.navigate(CurioRoutes.HOME) {
            popUpTo(CurioRoutes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingNotificationSession
        pendingNotificationSession = null
        if (pending != null) {
            // v229 — PERMISSION CHECKER: if the grant came back denied AND
            // the system no longer shows the rationale, the prompt was
            // permanently dismissed ("Don't ask again") — re-launching it is
            // a silent no-op and the session would run with NO visible
            // timer at all (no shade notification, no chip). Surface the
            // app-styled guidance dialog instead; "Open settings" resumes
            // this same pending session via ON_RESUME.
            val permanentlyDenied = !granted &&
                !hasNotificationPermission(context) &&
                (context as? Activity)?.shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS
                ) == false
            if (permanentlyDenied) {
                pendingNotificationSession = pending
                showNotificationsBlockedDialog = true
                return@rememberLauncherForActivityResult
            }
            // Granted (or a plain first-time denial): start the service for
            // whatever can show right now — the live notification needs the
            // grant, but the FLOATING BUBBLE needs only the separate
            // "Display over other apps" permission, so denying
            // POST_NOTIFICATIONS must never silently kill the bubble too.
            // The service's render() picks what actually shows from the
            // current permission state. The browser hasn't opened yet
            // (proceed is deferred to here), so the activity is still
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
            // v229 — returned from the app-notification settings page after
            // the permanently-denied checker dialog: re-check and continue
            // the SAME pending session (grant → service starts; still denied
            // → session runs without the shade timer, bubble permitting).
            if (event == Lifecycle.Event.ON_RESUME && awaitingNotificationsSettings) {
                awaitingNotificationsSettings = false
                val pending = pendingNotificationSession
                pendingNotificationSession = null
                if (pending != null) {
                    if (AppPreferences.exploreServiceShouldRun(context)) {
                        ExploreSessionService.start(context, pending)
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

    /** Starts a timed explore session, opens the search page (chosen engine — YouTube for music), back to Home. */
    fun startExploreSession(topic: CurioTopic, searchUrl: String = buildExploreSearchUrl(topic)) {
        engaged = true
        // v29 — auto-copy the search query to the clipboard the moment the
        // user taps Explore / Watch in: the browser opens a web search, but
        // pasting the topic into Spotify / Apple Music's own search box is
        // the reliable way to find it inside those apps (they don't always
        // hand off an in-app search from a web link). No extra button — it
        // just lands there, with a short toast so the user knows.
        val query = buildExploreQuery(topic)
        runCatching {
            clipboard.setText(AnnotatedString(query))
            android.widget.Toast.makeText(
                context, "Copied \"$query\" to clipboard", android.widget.Toast.LENGTH_SHORT
            ).show()
        }
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
            // v132 — no reserved navbar slot (the bottom band is gone): the
            // backdrop fills the reveal's own full content bounds now.
            CurioWatermarkBackdrop(activeCat = cat)
        }

        // v132 — the sentiment pill floats over the page (the bottom band
        // is gone): it hides while the user scrolls DOWN and slides back in
        // on scroll-up so it never covers the content being read.
        val revealScroll = rememberScrollState()
        var sentimentPillHidden by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            var last = revealScroll.value
            snapshotFlow { revealScroll.value }.collect { value ->
                val delta = value - last
                last = value
                if (delta > 3f) sentimentPillHidden = true
                else if (delta < -3f) sentimentPillHidden = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(revealScroll)
        ) {
        // ── 1. Top bar (category chip + pin bookmark + close ✕) ────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // v212 — category chip removed from top bar; category + Favorite
            // now live in the bottom bar. Year pill stays in the top-left.
            Row(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val yearQual = resolved?.titleAndYearQualifier()?.second?.takeIf { it.isNotBlank() }
                if (yearQual != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = cat.categorySurface(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurioIcon(
                                name = CurioIcons.Schedule,
                                contentDescription = null,
                                tint = cat.categoryInk(),
                                size = 14.dp
                            )
                            Text(
                                text = yearQual,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = cat.categoryInk()
                            )
                        }
                    }
                }
            }

            // Pin for later — filled bookmark when pinned (category accent),
            // outline when not. Only meaningful once the topic has resolved.
            // v36 — theme-aware surface (category tint, every theme).
            Surface(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val topic = resolved ?: return@Surface
                    if (AppPreferences.isTopicPinned(context, cat.id, topic.name)) {
                        AppPreferences.unpinTopic(context, cat.id, topic.name)
                    } else {
                        AppPreferences.pinTopic(context, cat.id, topic.name)
                    }
                },
                shape = CircleShape,
                color = if (isPinned) cat.themedAccent()
                        else cat.categorySurface(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                CurioIcon(
                    name = if (isPinned) CurioIcons.Bookmark else CurioIcons.BookmarkBorder,
                    contentDescription = if (isPinned) "Unpin this topic" else "Pin this topic for later",
                    tint = if (isPinned) cat.onAccent() else MaterialTheme.colorScheme.onSurface,
                    // v51 — slightly larger corner controls (24dp glyph on a
                    // 42dp circle).
                    size = 24.dp,
                    modifier = Modifier.padding(9.dp)
                )
            }

            // Close — return to the Spin deck (not Home): the landed card
            // keeps its "Tap to open" state so it can be reopened until the
            // user spins again or explores it (v5.6).
            // v36 — theme-aware surface (category tint, every theme).
            Surface(
                onClick = {
                    // Browse mode is read-only: nothing is ever recorded.
                    if (!browseMode && !engaged) {
                        resolved?.let { ExploreSessionStore.recordUnexplored(context, cat.id, it.name) }
                    }
                    navController.popBackStack()
                },
                shape = CircleShape,
                color = cat.categorySurface(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                CurioIcon(
                    name = CurioIcons.Close,
                    contentDescription = "Close and return to the deck",
                    tint = MaterialTheme.colorScheme.onSurface,
                    // v51 — slightly larger corner controls (24dp glyph on a
                    // 42dp circle).
                    size = 24.dp,
                    modifier = Modifier.padding(9.dp)
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
                        // v135 — an unresolvable legacy topic (renamed under
                        // a saved entry) still shows its own name, not the
                        // category name.
                        fallbackName = topicName,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                            // ── 2.5 Tags row — directly below the hero ──────────
                // v132 — the tags moved out of the (now removed) bottom band
                // back into the scroll body, right below the hero card:
                // hero → tags → actions → teaser → prompt.
                // v29 — when the topic carries reading/watching progress
                // (books: pages, anime: episodes) the floating progress
                // button straddles the hero's bottom edge, so the tags row
                // drops a little lower to stay clear of it.
                // v135 — tags OR the derived decade chip (a tag-less topic
                // with a recoverable year still gets its chip row).
                val hasTags = !resolved?.tags.isNullOrEmpty() || resolved?.derivedDecadeTag() != null
                val progressFloatGap = if (resolved?.progressTarget != null) 40.dp else 16.dp
                if (hasTags) {
                    RevealContentEntrance(delayMillis = 40) {
                        TagsRow(
                            cat = cat,
                            resolved = resolved,
                            modifier = Modifier.padding(top = progressFloatGap)
                        )
                    }
                }

                // ── 2.55 Book info section (books only) ──────────────────
                // Shows book poster, synopsis, and chapter chips for BOOKS
                // topics. v315 — gated on [contentUiReady] so it composes only
                // AFTER the shared-element morph settles; the poster Coil
                // decode + chapter LazyRow otherwise compete with the card
                // expansion and stall its frames.
                val bookTopic = resolved
                if (bookTopic != null && contentUiReady && bookTopic.categoryId == CategoryId.BOOKS &&
                    (bookTopic.synopsis != null || !bookTopic.chapters.isNullOrEmpty())) {
                    RevealContentEntrance(delayMillis = 60) {
                        BookInfoSection(
                            cat = cat,
                            topic = bookTopic,
                            onSynopsisClick = { showSynopsisDialog = true },
                            onChapterClick = { selectedChapter = it },
                            modifier = Modifier.padding(top = if (hasTags) 16.dp else progressFloatGap)
                        )
                    }
                }

                // ── 2.56 Album track-list section (albums only) ─────────
                // Mirrors the book section: an album poster + track-list card
                // (with a preview + "View the full track list") plus a track
                // chip row that jumps straight into the sheet at a track.
                // Albums carry `tracks` (number/title/duration) in the JSON;
                // gated on [contentUiReady] like the book section so the
                // poster lookup never competes with the card morph.
                val albumTopic = resolved
                if (albumTopic != null && contentUiReady && albumTopic.categoryId == CategoryId.ALBUMS &&
                    !albumTopic.tracks.isNullOrEmpty()) {
                    RevealContentEntrance(delayMillis = 60) {
                        AlbumInfoSection(
                            cat = cat,
                            topic = albumTopic,
                            onOpenSheet = { showAlbumSheet = true },
                            onTrackClick = { selectedAlbumTrack = it },
                            modifier = Modifier.padding(top = if (hasTags) 16.dp else progressFloatGap)
                        )
                    }
                }

                // ── 2.57 Series episode-list section (series only) ──────
                // Mirrors the book + album sections: a poster card with the
                // synopsis preview and episode count; tapping it opens the
                // full-height episode-list sheet (v350). Gated on
                // [contentUiReady] like the book/album sections so the poster
                // lookup never competes with the card morph.
                val seriesTopic = resolved
                if (seriesTopic != null && contentUiReady && seriesTopic.categoryId == CategoryId.SERIES &&
                    !seriesTopic.episodes.isNullOrEmpty()) {
                    RevealContentEntrance(delayMillis = 60) {
                        SeriesInfoSection(
                            cat = cat,
                            topic = seriesTopic,
                            onOpenSheet = { showSeriesSheet = true },
                            modifier = Modifier.padding(top = if (hasTags) 16.dp else progressFloatGap)
                        )
                    }
                }

                // ── 2.6 Action row — Express yourself / Explore ──────────────
                // v8.57 — the actions moved OUT of the bottom dock to sit
                // right below the hero card: always visible, no scaffold.
                RevealContentEntrance(delayMillis = 40) {
                    RevealActionRow(
                        cat = cat,
                        browseMode = latestBrowseMode,
                        resolved = latestResolved,
                        onExplore = latestOnExplore,
                        onAlready = latestOnAlready,
                        modifier = Modifier.padding(top = if (hasTags) 16.dp else progressFloatGap)
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
                // v135 — only rendered once the topic resolves: an
                // unresolvable legacy topic shows its name + actions instead
                // of a permanent "Loading topic…" placeholder.
                val teaserTopic = resolved
                if (teaserTopic != null) {
                    RevealContentEntrance(delayMillis = 160) {
                        TeaserCard(
                            cat = cat,
                            teaser = teaserTopic.teaser,
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                }

                // ── 6. Action prompt card ──────────────────────────────────
                val actionTopic = resolved
                if (actionTopic != null) {
                    RevealContentEntrance(delayMillis = 210) {
                        ActionPromptCard(
                            cat = cat,
                            action = actionTopic.exploreAction,
                            subtype = actionTopic.subtype,
                            modifier = Modifier.padding(top = 14.dp),
                            // v221 — for QUOTES, show the full quote text.
                            instructionOverride = if (actionTopic.categoryId == CategoryId.QUOTES) actionTopic.name else null
                        )
                    }
                }

                // Bottom clearance — the floating Like/Dislike pill overlays
                // the bottom of the page, so the last card clears it when
                // scrolled to the end (the pill is hidden while scrolling
                // down, but slides back in on the way up).
                Spacer(Modifier.height(100.dp))
            }

        }

        // ── Floating Category + Favorite bar (v212) ──────────────────────
        // Replaces the old Like/Dislike pill: category icon + name on the
        // left (expands on favorite), favorite star on the right. Slides
        // away on scroll-down, back on scroll-up. Now also visible in
        // Browse-Topics so users can favorite/share from the topic browser.
        val floatingTopic = resolved
        if (floatingTopic != null) {
            // v292 — TOPIC SHARE: tapping the Share pill in the floating
            // bar opens the customizable topic share card sheet.
            var showShareSheet by remember { mutableStateOf(false) }
            SideEffect {
                SentimentPillHost.content = {
                    AnimatedVisibility(
                        visible = !sentimentPillHidden,
                        enter = slideInVertically(
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ) { it } + fadeIn(animationSpec = tween(220)),
                        exit = slideOutVertically(
                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                        ) { it } + fadeOut(animationSpec = tween(180))
                    ) {
                        RevealCategoryFavoriteBar(
                            cat = cat,
                            isFavorited = sentiment == AppPreferences.SENTIMENT_LIKE,
                            accent = cat.themedAccent(),
                            ink = cat.onAccent(),
                            container = curioFloatingNavContainerFor(cat.categoryBackgroundWash()),
                            onShare = { showShareSheet = true },
                            onFavorite = {
                                AppPreferences.setTopicSentiment(
                                    context, cat.id, floatingTopic.id,
                                    if (sentiment == AppPreferences.SENTIMENT_LIKE)
                                        AppPreferences.SENTIMENT_NONE
                                    else AppPreferences.SENTIMENT_LIKE
                                )
                            }
                        )
                    }
                }
            }
            DisposableEffect(Unit) {
                onDispose { SentimentPillHost.content = null }
            }
            // v325 — LEAVING the reveal screen resets this topic's saved
            // share-card edits, so the next share starts clean (accidental
            // sheet exits meanwhile resume — the sheet persists on dismissal).
            DisposableEffect(floatingTopic) {
                onDispose {
                    floatingTopic?.let { AppPreferences.clearShareCardEdits(context, it.name) }
                }
            }
            if (showShareSheet) {
                com.curio.app.ui.components.TopicShareSheet(
                    topicName = floatingTopic.name,
                    categoryName = cat.displayName,
                    categoryGlyph = cat.iconGlyph,
                    accent = cat.themedAccent(),
                    quickFact = if (cat.id.name == "QUOTES") floatingTopic.name else floatingTopic.teaser,
                    authority = "${context.packageName}.fileprovider",
                    context = context,
                    onDismiss = { showShareSheet = false },
                    categoryFamily = cat.family,
                    topicByline = floatingTopic.byline,
                    // v328 — BOOK share cards: hand the chapters so the
                    // editor can offer Reading progress / Chapter review.
                    bookChapters = if (cat.id == CategoryId.BOOKS) floatingTopic.chapters.orEmpty()
                                   else emptyList(),
                    // v334 — the book's authored cover + fetched star rating,
                    // so the share card can show the cover (fetch/override in
                    // the editor) and the ★ row without a second lookup.
                    bookImageUrl = if (cat.id == CategoryId.BOOKS) floatingTopic.imageUrl else "",
                    bookRating = AppPreferences.bookRatingsState[floatingTopic.name]?.takeIf { it > 0.0 },
                    bookRatingCount = AppPreferences.bookRatingsCountState[floatingTopic.name] ?: 0
                )
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

    // v315/v316b/v348 — the book notes UI is ONE ModalBottomSheet (album
    // style): a collapsible "About this book" synopsis card at the top with
    // the full chapter list below it as expandable rows, so nothing requires
    // a tab switch. What you tapped seeds the sheet: the synopsis card opens
    // with the synopsis pre-expanded, a chapter chip expands that chapter
    // and scrolls to it.
    val bookSheetTopic = resolved
    if (bookSheetTopic != null && bookSheetTopic.categoryId == CategoryId.BOOKS &&
        (showSynopsisDialog || selectedChapter != null) &&
        (!bookSheetTopic.synopsis.isNullOrBlank() || !bookSheetTopic.chapters.isNullOrEmpty())
    ) {
        BookNotesSheet(
            cat = cat,
            topic = bookSheetTopic,
            mode = if (showSynopsisDialog) BookNotesMode.SYNOPSIS else BookNotesMode.CHAPTERS,
            chapter = selectedChapter,
            onSelectChapter = { selectedChapter = it },
            onDismiss = {
                showSynopsisDialog = false
                selectedChapter = null
            }
        )
    }

    // v332 — the album track-list UI mirrors the book notes sheet: one
    // full-height ModalBottomSheet hosting the album's complete track list
    // (number/title/duration) with the artwork up top. A track chip on the
    // reveal section opens it scrolled to that track.
    val albumSheetTopic = resolved
    if (albumSheetTopic != null && albumSheetTopic.categoryId == CategoryId.ALBUMS &&
        (showAlbumSheet || selectedAlbumTrack != null) &&
        !albumSheetTopic.tracks.isNullOrEmpty()
    ) {
        AlbumNotesSheet(
            cat = cat,
            topic = albumSheetTopic,
            track = selectedAlbumTrack,
            onSelectTrack = { selectedAlbumTrack = it },
            onDismiss = {
                showAlbumSheet = false
                selectedAlbumTrack = null
            }
        )
    }

    // v350 — the series episode-list sheet (album-style, mirrors the book /
    // album sheets): poster header + favorite heart, watched-progress rail,
    // the synopsis accordion, then the episodes grouped by season.
    val seriesSheetTopic = resolved
    if (seriesSheetTopic != null && seriesSheetTopic.categoryId == CategoryId.SERIES &&
        showSeriesSheet && !seriesSheetTopic.episodes.isNullOrEmpty()
    ) {
        EpisodeNotesSheet(
            cat = cat,
            topic = seriesSheetTopic,
            onDismiss = { showSeriesSheet = false }
        )
    }

    if (showOverlayPermissionDialog) {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
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
                }) {
                    Text("Allow", color = curioDialogActionColor())
                }
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
                }) { Text("Not now", color = curioDialogActionColor()) }
            }
        )
    }

    // v229 — POST_NOTIFICATIONS permanently-denied checker: the runtime
    // prompt can never come back after "Don't ask again", so without this
    // guidance the explore session silently ran with no visible timer.
    if (showNotificationsBlockedDialog) {
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
            onDismissRequest = {
                showNotificationsBlockedDialog = false
                val s = pendingNotificationSession
                pendingNotificationSession = null
                if (s != null) continueExploreFlow(s)
            },
            title = { Text("Notifications are off") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "The live explore timer (the shade notification with " +
                        "the progress bar and pause/cancel buttons) needs " +
                        "notification permission, which was turned off for " +
                        "Curio earlier."
                    )
                    Text(
                        "You can still explore without it. To bring the timer " +
                        "back, allow notifications in system settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationsBlockedDialog = false
                    val s = pendingNotificationSession
                    if (s != null) {
                        val launched = runCatching {
                            awaitingNotificationsSettings = true
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        }.isSuccess
                        if (!launched) {
                            awaitingNotificationsSettings = false
                            pendingNotificationSession = null
                            continueExploreFlow(s)
                        }
                    }
                }) {
                    Text("Open settings", color = curioDialogActionColor())
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotificationsBlockedDialog = false
                    val s = pendingNotificationSession
                    pendingNotificationSession = null
                    if (s != null) continueExploreFlow(s)
                }) { Text("Start anyway", color = curioDialogActionColor()) }
            }
        )
    }

    if (showExploreDialog && resolved != null) {
        val topic = resolved!!
        // v41 — the `action` val (verb/duration copy) was removed with the
        // dialog's helper paragraphs; the pills only need the service glyphs.
        // v27s — music topics (Album / Artist / Song) route the second pill
        // to the user's chosen music service; everything else stays YouTube.
        val musicTopic = topic.isMusicTopic()
        val watchService = MusicService.fromId(AppPreferences.musicServiceState)
        // v106 — the watch pill wears the service's brand logo (a
        // VectorDrawable that keeps its own brand colors). Non-music topics
        // keep YouTube. The old Material-glyph stand-ins ([MusicService
        // .brandTile]) are retired from the dialog.
        val watchBrandRes = if (musicTopic) {
            watchService.brandRes
        } else {
            R.drawable.ic_music_youtube
        }
        // v109 — music topics open an AUDIO service (Apple Music / Spotify /
        // YouTube Music): the pill says "Listen in". Only YouTube (video) —
        // and non-music topics, which always search YouTube — keeps "Watch in".
        val watchLabel = if (musicTopic && watchService != MusicService.YOUTUBE) {
            "Listen in"
        } else {
            "Watch in"
        }
        // v27u — the two pills are VISIBLE soft-tinted pills (the old
        // TextButton had no container color, so the pill shape was
        // invisible): an opaque lerp of the dialog action ink over the
        // dialog container.
        // v108 — dark mode: the pills wear the FILTER CHIPS' dark raised
        // glass (near-black tinted surface + One UI edge/glow + 4dp lift)
        // so the explore actions match the app's chip family at night.
        val pillInk = curioDialogActionColor()
        val pillShape = RoundedCornerShape(50)
        val pillFill = if (isCurioDarkTheme()) {
            lerp(MaterialTheme.colorScheme.surfaceContainerHigh, Color.Black, 0.15f)
        } else {
            lerp(curioDialogContainerColor(), pillInk, 0.14f)
        }
        // The glow's accent: the topic's own category shade (the filter
        // chips glow with their lane accent too).
        val pillGlowAccent = cat.themedAccent()
        // v11 — the dialog wears the shared Curio dialog theme: the card-
        // matching 24dp shape, the pastel-aware container, and the readable
        // action ink (deep rose on light/pastel so the buttons never wash
        // out, device primary in Material and dark).
        AlertDialog(
            containerColor = curioDialogContainerColor(),
            shape = CurioDialogShape,
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
            title = {
                Text(
                    "Explore ${topic.name}?",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                // v41 — the dialog is a single line now: the two helper
                // paragraphs (the engine/verb intro and the timed-explore
                // note) are gone, leaving the title, the pledge, and the two
                // pill actions. The pledge is the approved user rephrase —
                // no em dash, natural voice.
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Keep your research yours. Read the real sources instead of AI summaries, and the discovery is all yours.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // ── v22/v23 — opt-in for the floating explore bubble ──
                    // v23 — hidden from the dialog by default; the
                    // Notifications toggle re-shows it as a single main text
                    // line with no subtext.
                    if (AppPreferences.isShowBubbleOptInDialog(context)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CurioIcon(
                                name = CurioIcons.BubbleChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                size = 18.dp
                            )
                            Text(
                                "Show the explore bubble",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(checked = bubbleOptIn, onCheckedChange = { bubbleOptIn = it })
                        }
                    }
                }
            },
            confirmButton = {
                // v27r — the two explore actions are PILL-shaped buttons, each
                // a leading icon + short label so nothing wraps or truncates
                // in the width-constrained dialog: the globe (travel_explore)
                // searches the user's chosen engine, the rounded play tile
                // (youtube_activity) searches YouTube. v27u — the pills are
                // now VISIBLE (soft tinted container fill, no brand tiles),
                // 12dp apart.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = {
                            engaged = true
                            showExploreDialog = false
                            // v22 — apply the bubble opt-in before starting:
                            // opting in is an explicit intent, so a previously
                            // declined "Display over other apps" ask re-opens
                            // (mirrors the Settings toggle's behavior). Only
                            // applied when the dialog row is visible — when
                            // it's hidden, Settings owns the bubble entirely.
                            if (AppPreferences.isShowBubbleOptInDialog(context)) {
                                AppPreferences.setOverlayBubbleEnabled(context, bubbleOptIn)
                                if (bubbleOptIn) AppPreferences.setOverlayAskDeclined(context, false)
                            }
                            startExploreSession(topic, buildEngineSearchUrl(topic))
                        },
                        colors = curioDialogActionButtonColors(containerColor = pillFill),
                        shape = pillShape,
                        // v108 — the filter-chip pill recipe in dark: a 4dp
                        // lift + the One UI edge/glow (no-ops in light).
                        modifier = Modifier
                            .shadow(4.dp, pillShape)
                            .curioDarkGlow(4.dp, pillShape)
                            .curioGlassEdge(pillShape)
                            .curioInnerGlow(pillShape, pillGlowAccent, strength = 0.12f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // v27u — clean globe glyph for the user's chosen engine.
                        CurioIcon(
                            name = CurioIcons.TravelExplore,
                            contentDescription = null,
                            tint = pillInk,
                            size = 20.dp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Explore",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = {
                            engaged = true
                            showExploreDialog = false
                            if (AppPreferences.isShowBubbleOptInDialog(context)) {
                                AppPreferences.setOverlayBubbleEnabled(context, bubbleOptIn)
                                if (bubbleOptIn) AppPreferences.setOverlayAskDeclined(context, false)
                            }
                            // v27s — music topics open the chosen music
                            // service; everything else searches YouTube.
                            // v52b/v358 — Apple Music only opens ITEM pages
                            // natively (search links show an in-app browser
                            // banner) and Spotify needs a real album/track
                            // id for a deep link, so resolve the topic to a
                            // real catalog item first (Spotify only when its
                            // optional client id+secret are configured) and
                            // fall back to the search link when the lookup
                            // fails.
                            if (musicTopic &&
                                (watchService == MusicService.APPLE_MUSIC ||
                                    watchService == MusicService.SPOTIFY)
                            ) {
                                revealScope.launch {
                                    val deep = if (watchService == MusicService.APPLE_MUSIC)
                                        resolveAppleMusicItemUrl(topic)
                                    else
                                        resolveSpotifyItemUrl(topic)
                                    startExploreSession(
                                        topic,
                                        deep ?: buildMusicServiceSearchUrl(topic, watchService)
                                    )
                                }
                            } else {
                                startExploreSession(
                                    topic,
                                    if (musicTopic) buildMusicServiceSearchUrl(topic, watchService)
                                    else buildYouTubeSearchUrl(topic)
                                )
                            }
                        },
                        colors = curioDialogActionButtonColors(containerColor = pillFill),
                        shape = pillShape,
                        // v108 — the filter-chip pill recipe in dark: a 4dp
                        // lift + the One UI edge/glow (no-ops in light).
                        modifier = Modifier
                            .shadow(4.dp, pillShape)
                            .curioDarkGlow(4.dp, pillShape)
                            .curioGlassEdge(pillShape)
                            .curioInnerGlow(pillShape, pillGlowAccent, strength = 0.12f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // v106 — the service's brand logo (no tint — the
                        // logos keep their own brand colors).
                        Image(
                            painter = painterResource(watchBrandRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            watchLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            dismissButton = null
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
                containerColor = curioDialogContainerColor(),
                shape = CurioDialogShape,
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
                    TextButton(
                        onClick = {
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
                        },
                        colors = curioDialogActionButtonColors()
                    ) { Text("Start new explore") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            // Save the new topic for later — the current session
                            // keeps running untouched.
                            val s = pendingConflictSession
                            showConflictDialog = false
                            pendingConflictSession = null
                            conflictActiveSession = null
                            if (s != null) {
                                AppPreferences.pinTopic(context, s.categoryId, s.topicName)
                            }
                        },
                        colors = curioDialogActionButtonColors()
                    ) { Text("Save for later") }
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
        // v43 — bumped a notch (+1.5sp per tier) so the pair's labels read
        // bolder and more prominent without growing off their single lines.
        textSp = if (narrow) 14.5.sp else if (compact) 15.5.sp else 17.5.sp,
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
                    cat = cat,
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
                } else {
                    // v25 — browse-mode Explore runs the REAL explore session
                    // (dialog → timer → recents → done-mark), same as the
                    // Spin deck — no more silent out-of-app search.
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
                            onClick = onExplore
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
    // v10 — fixed height matches the paired "Express yourself" button so
    // the two actions read as a unified row instead of mismatched siblings.
    val startShape = RoundedCornerShape(50)
    val contentInk = cat.themedButtonInk()
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            onClick()
        },
        enabled = enabled,
        shape = startShape,
        colors = curioButtonColors(
            // v8.57 — themed to the category accent so the button wears the
            // lane's own color instead of the generic theme primary. v9.x —
            // AMOLED overrides to pitch-black (accent becomes the edge
            // shine); Material wears the device primary with the accent rim.
            containerColor = cat.themedButtonFill(),
            contentColor = contentInk,
            disabledContainerColor = cat.themedButtonFill().copy(alpha = 0.35f),
            disabledContentColor = contentInk.copy(alpha = 0.45f)
        ),
        contentPadding = PaddingValues(
            horizontal = metrics.startPadH,
            vertical = metrics.startPadV
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .categoryEdgeShine(startShape, accent = cat.themedAccent())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(metrics.gap)
        ) {
            CurioIcon(
                CurioIcons.AutoAwesome,
                null,
                tint = contentInk,
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
    cat: CurioCategory,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    metrics: RevealDockMetrics,
    onClick: () -> Unit
) {
    // v8.49 — text-style writing action on the transparent inline row.
    // v8.55 — the tier metrics resize it with the pill.
    // v11 — SOLID surface: the ghost text button washed out next to the
    // filled CTA, so it now wears a real theme-aware background (the same
    // tinted card surface as the rest of the page) with readable category
    // ink, a hairline edge, and the category edge shine. In Material style
    // categorySurface falls back to the device surface so it stays a proper
    // Material control.
    val surface = cat.categorySurface()
    val ink = if (enabled) {
        cat.categoryInk()
    } else {
        cat.categoryInk().copy(alpha = 0.40f)
    }
    val haptics = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(50),
        // v27n — the disabled fill is OPAQUE too (the 45% alpha let the
        // elevation shadow bleed through): an opaque blend toward the page
        // background keeps the muted disabled look.
        color = if (enabled) surface
                else lerp(MaterialTheme.colorScheme.background, surface, 0.45f),
        shadowElevation = 3.dp,
        modifier = modifier
            // v28 — dark mode elevation visibility (glow + hairline).
            // v114 — the dark-mode edge must match the pill family:
            // `categoryEdgeShine` painted a full-width band that peeked past
            // the capsule's rounded ends — `curioGlassEdge` hugs the shape
            // (same as the reveal explore pills / filter chips).
            .curioDarkGlow(3.dp, RoundedCornerShape(50))
            .curioGlassEdge(RoundedCornerShape(50))
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
                // v37 — ExtraBold to match the filled Start exploring CTA's
                // weight so the pair reads as a unified action row.
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = metrics.textSp,
                    fontWeight = FontWeight.ExtraBold
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
private val RevealHeroBaseHeightPortrait = 260.dp
/** Landscape hero — shorter to leave room for content below. */
private val RevealHeroBaseHeightLandscape = 180.dp

@Composable
private fun HeroCard(
    cat: com.curio.app.data.CurioCategory,
    resolved: CurioTopic?,
    modifier: Modifier = Modifier,
    // v135 — shown when the topic can't be resolved (renamed under a
    // saved entry) so the page never reads as "the wrong category".
    fallbackName: String = ""
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
    // v28 — the reveal hero uses the SAME accent source as the Spin ticket
    // (themedAccent, NOT the headerAccent() deepen): the hero morphs out of
    // the ticket, so its gradient must read pixel-identical. The old
    // headerAccent() deepen made the reveal hero a shade darker than the
    // ticket in LIGHT mode (dark's 0.94 factor hid it).
    val accent = cat.themedAccent()
    // v28 — mirror the Spin ticket's gradient recipe EXACTLY in every
    // theme: pastel light uses the ticket's pastel crown + on-hue tint
    // stops; everything else uses the shared card gradient. The old
    // cardGradient-only recipe diverged from the ticket in pastel light
    // (the ticket's second stop IS the tint, cardGradient's is only 30%
    // toward it) — the morph visibly shifted color on light pastel pages.
    val heroGradient = if (AppPreferences.pastelColorsState) {
        if (isCurioDarkTheme()) {
            // v81 — dark: the muted DEEP pastel twin over black (never the
            // airy light pastel, which would glare on the pitch-black page).
            listOf(
                lerp(accent, Color.Black, 0.05f),
                lerp(accent, Color.Black, 0.28f)
            )
        } else {
            listOf(
                lerp(accent, Color.Black, 0.05f),
                lightAccentTint(accent, saturation = 0.22f, lightness = 0.80f)
            )
        }
    } else {
        CurioGradients.cardGradient(accent)
    }
    // v7.5 — pastel mode lightens the hero gradient, so the pill content
    // flips from white to the deep accent (light) / light twin (dark).
    // Match the Spin ticket's ink formula exactly so the morph reads as
    // the same card expanding: pastel → pastelFillInk, else → onAccent.
    val ink = if (AppPreferences.pastelColorsState) pastelFillInk(accent) else cat.onAccent()

    // v132 — the hero pills (action badge, byline, subtype) wear the SAME
    // recipe as the Spin main card's pills: the card ink at 18% over the
    // gradient, so the reveal hero reads as the same card in light AND dark
    // (the old opaque frosted glass read as white blobs next to the
    // ticket's subtle tint).

    // ── Gradient brush — match the Spin ticket's formula so the card
    //    reads as the same surface during the morph. When heroGradientOn
    //    is enabled, the hero gets the same top-lit diagonal sweep as the
    //    deck's front ticket; otherwise a plain vertical gradient.
    val pastelLightHero = AppPreferences.pastelColorsState
    // v25 — the Enhanced main gradient experiment PASSED: always ON, so its
    // toggle was removed from Experiments and the read is hardcoded here.
    val heroGradientOn = true
    // v27u — the hero's gradient rim border was removed (it mirrored the
    // Spin ticket's border, which is also gone — the morph stays clean).
    // v24 — the dual-accent hero gradient experiment was rejected (ugly
    // golden blend); always OFF, so the blend branch below is dead.
    val heroBlendOn = false

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
    val revealHeroBase = if (windowWidthSizeClass().isWide) RevealHeroBaseHeightLandscape else RevealHeroBaseHeightPortrait
    val heroHeight by animateDpAsState(
        targetValue = revealHeroBase +
            with(density) { settledOverflowPx.toDp() },
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "revealHeroHeight"
    )

    // v29 — the hero sits in a Box so the floating progress button can
    // straddle the card's bottom edge (half on the hero, half floating in
    // the gap above the action row). Only rendered for topics that carry a
    // progress target (books: pages, anime: episodes).
    Box(modifier = modifier.fillMaxWidth()) {
    Surface(
        modifier = Modifier
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
                // v92 — the One UI "shiny edge" (same as the Spin ticket,
                // so the shared-element morph stays pixel-identical), dark
                // only.
                .curioGlassEdge(RoundedCornerShape(30.dp))
        ) {
            // ── Gradient brush — pixel-perfect match with the Spin ticket:
            //    same color stops AND the same diagonal linearGradient when
            //    heroGradientOn is enabled, so every pixel reads identical
            //    during the morph.
            val density = LocalDensity.current
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            // v15 — the hero brush mirrors the Spin ticket exactly so the
            // shared-element morph stays pixel-identical.
            val heroBrush = if (heroBlendOn) {
                // v10 — dual-accent blend: category accent meets a warm
                // golden companion in a multi-stop vertical gradient.
                Brush.verticalGradient(CurioGradients.heroBlendGradient(accent))
            } else if (heroGradientOn) {
                val crown = lerp(heroGradient.first(), Color.White, if (pastelLightHero) 0.08f else 0.16f)
                val base = lerp(heroGradient.last(), Color.Black, 0.06f)
                val stops = if (heroGradient.size > 2) {
                    listOf(crown) + heroGradient.drop(1).dropLast(1) + listOf(base)
                } else {
                    // v87 — OKLab interpolation (same stops as the Spin ticket
                    // so the shared-element morph stays pixel-identical).
                    oklabGradientStops(crown, base, 3)
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
                // ── Top — the byline pill (v141) — the SAME top-left
                // corner the Spin ticket wears, so the shared-element morph
                // reads as the pill staying put while the card grows. The
                // YEAR qualifier ("Moby-Dick (1851)" → "Moby-Dick" + an
                // "1851" pill) moved OUT of the hero in v146: it rides next
                // to the category chip in the top bar, because the progress
                // pill sits at the hero's top-right and a long byline pushed
                // the year pill underneath it. Same pill recipe as the
                // ticket (ink at 18%, labelMedium bold, h12/v6 padding) so
                // the byline morph is pixel-identical.
                val byline = resolved?.byline?.takeIf { it.isNotBlank() }
                val bylineLabel = when (cat.id) {
                    CategoryId.ALBUMS -> "Artist"
                    CategoryId.BOOKS -> "Author"
                    CategoryId.SERIES -> "Created by"
                    CategoryId.FILMS -> "Director"
                    CategoryId.ANIMATED_MOVIES -> "Director"
                    CategoryId.ARTWORKS -> "Painter"
                    CategoryId.DISCOVERIES -> "Discovered by"
                    CategoryId.QUOTES -> "Author"
                    else -> null
                }
                if (byline != null && bylineLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ink.copy(alpha = 0.18f),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "$bylineLabel · $byline",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // v327 — BOOKS: on-demand keyless star rating, next to the
                // author pill. The reveal fetches the average rating the
                // first time a book opens (if the Settings hub hasn't cached
                // it) and shows a ★ chip — the fetch had no visible view.
                if (cat.id == CategoryId.BOOKS) {
                    val bookName = resolved?.name?.takeIf { it.isNotBlank() } ?: fallbackName
                    val bookRating = AppPreferences.bookRatingsState[bookName]
                    val bookCount = AppPreferences.bookRatingsCountState[bookName] ?: 0
                    // LocalContext.current is @Composable — read it here, in
                    // the composable scope, NOT inside the LaunchedEffect
                    // body (that lambda is suspend-only and would fail CI).
                    val context = LocalContext.current
                    // v333 — the on-demand ★ rating also respects the book-
                    // fetch consent toggle (it hits Google Books).
                    val ratingFetchConsent = AppPreferences.bookFetchEnabledState
                    LaunchedEffect(bookName, resolved?.byline, ratingFetchConsent) {
                        if (ratingFetchConsent && bookRating == null && bookName.isNotBlank()) {
                            val stars = BookCoverFetch.fetchRatingFor(bookName, resolved?.byline)
                            if (stars != null && stars.average > 0.0) {
                                AppPreferences.setBookRatingWithCount(
                                    context, bookName, stars.average, stars.count
                                )
                            }
                        }
                    }
                    if (bookRating != null && bookRating > 0.0) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ink.copy(alpha = 0.18f),
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CurioIcon(
                                    name = CurioIcons.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color(0xFFF6B23B),
                                    size = 13.dp
                                )
                                Text(
                                    text = String.format("%.1f", bookRating),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ink
                                )
                                // v328 — show the ratings count too ("· 12k").
                                if (bookCount > 0) {
                                    Text(
                                        text = "· " + compactCount(bookCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ink.copy(alpha = 0.7f)
                                    )
                                }
                            }
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
                // v36 — the category eyebrow pill was removed: the top bar
                // already shows the category chip, so the hero title stands
                // alone (no duplicate category inside the card).
                Text(
                    // v141 — same as the ticket: a trailing year qualifier
                    // never lives in the title, so the two titles read
                    // identical during the morph. v146 — the year pill rides
                    // in the top bar next to the category chip (see above),
                    // out of the hero where the progress pill sits.
                    // v221 — for QUOTES, show the author name instead of the quote text.
                    text = if (resolved?.categoryId == CategoryId.QUOTES && resolved.byline.isNotBlank()) {
                        resolved.byline
                    } else {
                        resolved?.titleAndYearQualifier()?.first ?: fallbackName.ifBlank { cat.displayName }
                    },
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

                // ── Bottom — action + subtype pills, one per corner ─────
                // v141 — the action badge (verb + duration) lives here now;
                // the top-left corner wears the byline pill that matches the
                // Spin ticket for a 1:1 morph.
                val subtype = resolved?.subtype?.takeIf { it.isNotBlank() }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left slot — action pill (or a blank spacer so a lone
                    // subtype still pins to the right corner).
                    if (action != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ink.copy(alpha = 0.18f),
                            shadowElevation = 0.dp,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CurioIcon(
                                    name = verbIcon(action.verb),
                                    contentDescription = null,
                                    tint = ink,
                                    size = 14.dp
                                )
                                Text(
                                    text = "${action.verb} for ~${action.durationMinutes} min",
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
                    // action pill on its left corner when there's no subtype).
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

            // ── v29 — progress badge (pages read / episodes watched) at
            //    the hero's TOP-RIGHT corner: a small OPAQUE frosted pill
            //    (count only) that opens the progress editor on tap. The
            //    old long bottom-straddling control is gone — the corner
            //    badge never clips during the shared-element morph and
            //    its solid fill reads on any hero gradient, light or dark.
            val heroTopic = resolved
            if (heroTopic != null && heroTopic.progressTarget != null) {
                CurioProgressPill(
                    topic = heroTopic,
                    accent = cat.accent,
                    // v45 — the pill's text stays the deep accent (readable
                    // on the light frosted pill). v53 — the EDITOR dialog
                    // now wears the standard background tint. v66 — the
                    // dialog content is the READABLE category ink (deep
                    // accent in light, light twin in dark) so every element
                    // — ring, steppers, slider, Save — reads on the dialog
                    // in both modes instead of the raw accent going
                    // dark-on-dark.
                    ink = cat.accent,
                    background = if (isCurioDarkTheme()) lerp(cat.accent, Color.Black, 0.55f)
                    else lerp(cat.accent, Color.White, 0.85f),
                    showBar = false,
                    dialogContentColor = cat.categoryInk(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                )
            }
            } // inner background Box
        } // BoxWithConstraints
    } // HeroCard Surface
    } // HeroCard floating Box
}

// ════════════════════════════════════════════════════════���══════════════════
// Book info section — synopsis overlay + chapter chips (books only)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Book info section shown below the hero for BOOKS topics. Displays:
 * - Book poster image (fetched from imageUrl via Coil)
 * - Synopsis text in a scrollable overlay
 * - Chapter chips for browsing chapter summaries
 */
@Composable
private fun BookInfoSection(
    cat: com.curio.app.data.CurioCategory,
    topic: CurioTopic,
    onSynopsisClick: () -> Unit,
    onChapterClick: (BookChapter) -> Unit,
    modifier: Modifier = Modifier
) {
    val synopsis = topic.synopsis
    val chapters = topic.chapters
    val imageUrl = topic.imageUrl
    
    if (synopsis == null && chapters == null) return
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Book poster + synopsis row
        if (synopsis != null) {
            BookSynopsisCard(
                cat = cat,
                    synopsis = synopsis,
                    imageUrl = imageUrl,
                    bookTitle = topic.name,
                    author = topic.byline,
                    pageCount = topic.pageCount,
                    onClick = onSynopsisClick
            )
        }
        // v352 — only chapters with a real summary preview become chips on
        // the reveal row; blank-summary chapters stay in the sheet's full
        // list (they still open there) but no longer render empty boxes.
        val chipChapters = chapters.orEmpty().filter { it.summary.isNotBlank() }
        if (chipChapters.isNotEmpty()) {
            BookChapterChips(
                cat = cat,
                chapters = chipChapters,
                onChapterClick = onChapterClick
            )
        }
    }
}

/**
 * Synopsis card with book poster on the left and scrollable synopsis text.
 */
@Composable
private fun BookSynopsisCard(
    cat: com.curio.app.data.CurioCategory,
    synopsis: String,
    imageUrl: String,
    bookTitle: String,
    author: String,
    pageCount: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cat.categorySurface(MaterialTheme.colorScheme.surface),
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    CurioIcon(
                        name = CurioIcons.MenuBook,
                        contentDescription = null,
                        tint = cat.categoryInk(),
                        size = 16.dp,
                        modifier = Modifier.padding(7.dp)
                    )
                }
                Text(
                    text = "SYNOPSIS".uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = cat.categoryInk()
                )
                if (pageCount != null) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "$pageCount pages",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // v320 — a keyless-fetched average rating (from the Settings
                // book hub) rides this header as a compact star chip.
                val fetchedRating = AppPreferences.bookRatingsState[bookTitle]
                if (fetchedRating != null && fetchedRating > 0.0) {
                    if (pageCount != null) Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            CurioIcon(
                                name = CurioIcons.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFF6B23B),
                                size = 12.dp
                            )
                            Text(
                                text = String.format("%.1f", fetchedRating) +
                                    (if (AppPreferences.bookRatingsCountState[bookTitle]?.let { it > 0 } == true)
                                        " · ${compactCount(AppPreferences.bookRatingsCountState[bookTitle] ?: 0)}" else ""),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Poster + a 5-line synopsis PREVIEW. v316b — the page shows only
            // the opening lines (a teaser, like the chapters' 2-line previews);
            // tapping the card opens the book-notes sheet, which hosts the
            // FULL synopsis (and the chapters) in a tall scrollable sheet.
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                BookCoverPoster(
                    bookTitle = bookTitle,
                    imageUrl = imageUrl,
                    modifier = Modifier
                        .size(width = 80.dp, height = 120.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                )
                Text(
                    text = synopsis,
                    style = RevealEditorialBody,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            if (synopsis.length > 160) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Read the full synopsis →",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = cat.categoryInk(),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

/**
 * Estimate the real chapter count from grouped chapter titles.
 * Handles patterns like "Books IV–VIII" (5 chapters), "Chapters 1–10" (10 chapters),
 * "Chapter 3" (1 chapter), and Roman numeral ranges.
 */
private fun estimateChapterCount(chapters: List<com.curio.app.data.BookChapter>): Int {
    val romanNumerals = mapOf(
        "I" to 1, "V" to 5, "X" to 10, "L" to 50, "C" to 100, "D" to 500, "M" to 1000
    )

    fun romanToInt(s: String): Int {
        val str = s.trim().uppercase()
        var result = 0
        var prev = 0
        for (c in str.reversed()) {
            val v = romanNumerals[c.toString()] ?: continue
            result += if (v < prev) -v else v
            prev = v
        }
        return result
    }

    fun parseNumber(s: String): Int {
        val trimmed = s.trim()
        return trimmed.toIntOrNull() ?: romanToInt(trimmed)
    }

    var total = 0
    for (ch in chapters) {
        val title = ch.title
        // Match patterns: "Books X–Y", "Chapters X-Y", "Ch. X-Y", "Part X, Chapters Y-Z",
        // "Cantos X–Y", "Canto X"
        val rangePattern = Regex("(books?|chapters?|ch\\.?|parts?|cantos?|canto)\\s+([IVXLCDM0-9]+)[\u2013\\-–]+([IVXLCDM0-9]+)", RegexOption.IGNORE_CASE)
        val singlePattern = Regex("(books?|chapters?|ch\\.?|parts?|cantos?|canto)\\s+([IVXLCDM0-9]+)", RegexOption.IGNORE_CASE)

        val rangeMatch = rangePattern.find(title)
        if (rangeMatch != null) {
            val from = parseNumber(rangeMatch.groupValues[2])
            val to = parseNumber(rangeMatch.groupValues[3])
            if (from > 0 && to >= from) {
                total += (to - from + 1)
                continue
            }
        }

        val singleMatch = singlePattern.find(title)
        if (singleMatch != null) {
            total += 1
            continue
        }

        // No recognizable range — count as 1
        total += 1
    }
    return total
}

/**
 * Extract a short range label from a chapter title for the chip header.
 * E.g. "Books IV–VIII" → "Ch. IV–VIII", "Chapters 1–10" → "Ch. 1–10",
 * "Book I — The Quarrel" → "Ch. I", "Inferno, Cantos I–V" → "Ch. I–V".
 */
private fun chapterRangeLabel(ch: com.curio.app.data.BookChapter): String {
    val rangePattern = Regex(
        "(books?|chapters?|ch\\.?|parts?|cantos?|canto)\\s+([IVXLCDM0-9]+)[\u2013\\-–]+([IVXLCDM0-9]+)",
        RegexOption.IGNORE_CASE
    )
    val singlePattern = Regex(
        "(books?|chapters?|ch\\.?|parts?|cantos?|canto)\\s+([IVXLCDM0-9]+)",
        RegexOption.IGNORE_CASE
    )
    val rangeMatch = rangePattern.find(ch.title)
    if (rangeMatch != null) {
        val from = rangeMatch.groupValues[2]
        val to = rangeMatch.groupValues[3]
        return "Ch. $from–$to"
    }
    val singleMatch = singlePattern.find(ch.title)
    if (singleMatch != null) {
        return "Ch. ${singleMatch.groupValues[2]}"
    }
    return "Ch. ${ch.number}"
}

/**
 * Horizontal chapter chips showing chapter titles and page ranges.
 */
@Composable
private fun BookChapterChips(
    cat: com.curio.app.data.CurioCategory,
    chapters: List<com.curio.app.data.BookChapter>,
    onChapterClick: (BookChapter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                CurioIcon(
                    name = CurioIcons.AutoAwesome,
                    contentDescription = null,
                    tint = cat.categoryInk(),
                    size = 16.dp,
                    modifier = Modifier.padding(7.dp)
                )
            }
            Text(
                text = "CHAPTERS".uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = cat.categoryInk()
            )
            Text(
                text = "${estimateChapterCount(chapters)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Chapter chips row (scrollable)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            itemsIndexed(chapters) { index, chapter ->
                BookChapterChip(
                    cat = cat,
                    chapter = chapter,
                    index = index,
                    onClick = { onChapterClick(chapter) }
                )
            }
        }
    }
}

/**
 * Individual chapter chip with title, page range, and summary preview.
 */
@Composable
private fun BookChapterChip(
    cat: com.curio.app.data.CurioCategory,
    chapter: com.curio.app.data.BookChapter,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow),
        shadowElevation = 2.dp,
        modifier = modifier
            .width(160.dp)
            // v315 — the old 156dp boxes were taller than their 2-line previews
            // needed; they now sit just above the preview height (116-118dp).
            .height(118.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Chapter number — v320: compact "CH 3" (was "Ch. 3")
            Text(
                text = chapterDisplayLabel(chapter.number, chapter.title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = cat.categoryInk()
            )
            
            // Chapter title — one line, the preview below carries the detail
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Page range
            if (chapter.pageStart > 0 && chapter.pageEnd > 0) {
                Text(
                    text = "pp. ${chapter.pageStart}–${chapter.pageEnd}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Summary preview (2 lines)
            if (chapter.summary.isNotBlank()) {
                Text(
                    text = chapter.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * What opened the book-notes sheet: the synopsis card (SYNOPSIS) or a
 * chapter chip (CHAPTERS). v348 — the sheet itself is one album-style
 * scroll (synopsis accordion + chapter rows); the mode only seeds which
 * part is pre-expanded.
 */
private enum class BookNotesMode { SYNOPSIS, CHAPTERS }

/**
 * The book poster used on the reveal page AND inside the book-notes sheet —
 * answers the authored URL first, then the same Open Library title-cover
 * fallback [BookCoverFetch.coverUrlFor] resolves, so the disk-cache key is
 * shared with the Settings bulk cover fetch.
 */
@Composable
private fun BookCoverPoster(
    bookTitle: String,
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    // v352 — the last cover the hub's provider actually RESOLVED (e.g. a
    // Google Books thumbnail) rides between the authored URL and the bare
    // Open Library fallback, so hub-fetched covers appear on the reveal.
    // The state is read OUTSIDE remember so a hub fetch recomposes this.
    val resolvedUrl = AppPreferences.bookCoverUrlsState[bookTitle]?.takeIf { it.isNotBlank() }
    val coverCandidates = remember(bookTitle, imageUrl, resolvedUrl) {
        listOfNotNull(
            imageUrl.takeIf { it.isNotBlank() },
            resolvedUrl,
            "https://covers.openlibrary.org/b/title/${Uri.encode(bookTitle)}-M.jpg",
        ).distinct()
    }
    var coverIndex by remember(bookTitle, imageUrl) { mutableStateOf(0) }
    // v333 — honor the Settings book-fetch consent toggle (isBookFetchEnabled,
    // the same gate the bulk "Fetch covers" hub uses). With fetching OFF the
    // poster serves ONLY what Coil already holds in its memory/disk cache and
    // never reaches the network — the reveal was auto-downloading covers
    // (and the keyless fallback URL) on every book visit even when the user
    // never enabled fetching.
    val bookFetchConsent = AppPreferences.bookFetchEnabledState
    // v354/v356 — INPUT-SIDE fallback: when the toggle is ON but every static
    // candidate above failed (no authored URL, no hub-resolved cover, and
    // the Open Library guess 404s), the poster LIVE-resolves a cover and
    // persists it. v356 — iTunes is tried FIRST (keyless ebook search), then
    // Google Books, then LibraryThing (only when its free key is configured),
    // so the best keyless source wins before the older fallbacks.
    val context = LocalContext.current
    var liveFallbackDone by remember(bookTitle, imageUrl) { mutableStateOf(false) }
    LaunchedEffect(bookTitle, imageUrl, bookFetchConsent, coverIndex, liveFallbackDone) {
        // Fires only after EVERY static candidate has actually ERRORED
        // (onError bumps coverIndex past the list), never on first open.
        val exhausted = coverIndex >= coverCandidates.size
        if (bookFetchConsent && !liveFallbackDone && exhausted) {
            val providers = listOf(
                com.curio.app.features.settings.BookCoverFetch.BookCoverProvider.ITUNES,
                com.curio.app.features.settings.BookCoverFetch.BookCoverProvider.GOOGLE_BOOKS,
                com.curio.app.features.settings.BookCoverFetch.BookCoverProvider.LIBRARY_THING
            )
            var url: String? = null
            for (p in providers) {
                url = com.curio.app.features.settings.BookCoverFetch.resolveCoverUrl(
                    context, bookTitle, null, "", p
                )
                if (url != null) break
            }
            if (url != null) {
                AppPreferences.setBookCoverUrl(context, bookTitle, url)
                coverIndex = 0
            }
            liveFallbackDone = true
        }
    }
    // v338 — EMPTY-STATE FIX: the poster is a real two-tone book-plate with a
    // menu_book glyph BEHIND the loaded cover instead of an empty clipped
    // AsyncImage. When a cover is missing (no URL, fetch OFF with nothing
    // cached, or every candidate failed) there is now a visible, shadow-able
    // surface — the caller's shadow previously fell on a transparent hole
    // and read as a glitchy blur box. The cover (when it arrives) paints
    // OVER the plate, so the placeholder never competes with artwork.
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(
            name = CurioIcons.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            size = 26.dp
        )
        if (coverCandidates.isNotEmpty() && coverIndex < coverCandidates.size) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverCandidates[coverIndex])
                    .crossfade(true)
                    .networkCachePolicy(
                        if (bookFetchConsent) CachePolicy.ENABLED else CachePolicy.DISABLED
                    )
                    .build(),
                contentDescription = "Book cover",
                onError = { coverIndex += 1 },
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/** v328 — 1234 → "1.2k", 34500 → "34k" (ratings-count shorthand). */
private fun compactCount(n: Int): String = when {
    n < 1000 -> "$n"
    n < 10000 -> {
        val tenths = n / 100 // 1234 → 12 → "1.2"
        if (tenths % 10 == 0) "${tenths / 10}k" else "${tenths / 10}.${tenths % 10}k"
    }
    else -> "${n / 1000}k"
}

/**
 * v315/v316b — the book notes ModalBottomSheet (the slim centered dialog is
 * gone): ONE sheet hosts BOTH the synopsis AND the chapter reader. The cover
 * + title head the sheet; a segmented Synopsis | Chapters tab row switches
 * between them in place (opened from whichever card you tapped); the sheet
 * is tall (fills ~92% of the screen) with a scrollable body so neither the
 * full synopsis nor the chapter reader ever feels cramped.
 */
/** v340 — the book's OWN chapter label carried by the title ("Chapter 57",
 *  "Letter I", "Book II", "Canto 12", "Part 3", "Law 4"...) so the reader
 *  shows the real numbering instead of the positional index; untitled
 *  chapters fall back to the compact "CH N". */
private val BookChapterLabelRegex = Regex(
    "^(Chapter|CH|Ch\\.?|Letter|Letters?|Book|Canto|Law|Poem|Story|Part|Volume|Section|Act)\\s+(?:[IVX]+|\\d+|One|Two|Three|Four|Five|Six|Seven|Eight|Nine|Ten|Eleven|Twelve)\\b" +
        "|^(Conclusion|Epilogue|Prologue|Preface|Interlude|Afterword|Postscript|Coda)\\b",
    RegexOption.IGNORE_CASE
)

/** The label prefix of [title] when it self-labels ("Chapter 5 - The Danse
 *  Macabre" -> "Chapter 5"), else the positional "CH N". */
private fun chapterDisplayLabel(number: Int, title: String): String {
    val m = BookChapterLabelRegex.find(title)
    return m?.value ?: "CH $number"
}

@Composable
private fun BookNotesSheet(
    cat: com.curio.app.data.CurioCategory,
    topic: CurioTopic,
    mode: BookNotesMode,
    chapter: BookChapter?,
    onSelectChapter: (BookChapter) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    // v339 — cover-art palette: the sheet derives its FULL colour set
    // (background + cards + chips + text) from the BOOK cover's artwork
    // (null swatches → per-element category fallback). Honors the
    // book-fetch consent gate, so no network is touched when the toggle
    // is OFF (cached covers only). v352 — when the topic has no authored
    // cover, the hub's last RESOLVED cover URL drives the palette too.
    val paletteUrl = topic.imageUrl.takeIf { it.isNotBlank() }
        ?: AppPreferences.bookCoverUrlsState[topic.name]?.takeIf { it.isNotBlank() }
    var coverSwatches by remember(paletteUrl) { mutableStateOf<CoverSwatches?>(null) }
    LaunchedEffect(paletteUrl) {
        coverSwatches = fetchCoverSwatches(
            context,
            paletteUrl,
            networkAllowed = AppPreferences.bookFetchEnabledState
        )
    }
    val coverPal = cat.notesSheetPalette(coverSwatches)
    val accent = coverPal?.accent ?: cat.themedAccent()
    val onAccent = coverPal?.onAccent ?: cat.onAccent()
    val ink = coverPal?.ink ?: cat.categoryInk()
    val surface = coverPal?.surface ?: cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow)
    val surfaceHigh = coverPal?.surfaceHigh ?: cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
    val surfaceAlt = coverPal?.surfaceAlt ?: MaterialTheme.colorScheme.secondaryContainer
    val onSurfaceAlt = coverPal?.onSurfaceAlt ?: MaterialTheme.colorScheme.onSecondaryContainer
    val onSurface = coverPal?.onSurface ?: MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = coverPal?.onSurfaceVariant ?: MaterialTheme.colorScheme.onSurfaceVariant
    val chapters = topic.chapters.orEmpty()
    val hasSynopsis = !topic.synopsis.isNullOrBlank()
    val hasChapters = chapters.isNotEmpty()
    // v348 — ALBUM-STYLE single sheet (no Synopsis | Chapters tabs): the
    // synopsis is a collapsible "About this book" card pinned at the top of
    // the chapter list (mirroring the album sheet's "About this album"), and
    // every chapter is an expandable row below it. v352 — the synopsis
    // starts COLLAPSED whatever opened the sheet; a chapter chip still
    // expands + jumps straight to that chapter.
    var expandedNumber by rememberSaveable(chapters.size) {
        mutableStateOf<Int?>(null)
    }
    // v348 — favorite heart for the whole book (book-level, like the album
    // hearts). Reactive: tapping toggles AppPreferences and this recomposes.
    val bookName = topic.name
    val isFavBook = bookName in AppPreferences.bookFavoritesState
    // Reading progress (identical semantics to the old reader tab): the
    // number of chapters marked read; chapter N is read when N <= chaptersDone.
    val chaptersDone = AppPreferences.bookReadingProgressState[bookName] ?: 0
    // v352 — per-chapter Like hearts (book name → liked chapter numbers).
    val chapterLikes = AppPreferences.bookChapterLikesState[bookName].orEmpty()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    // v352 — chapter switching no longer lags: the seed chapter drives an
    // INSTANT scrollToItem (the old animated glide over a long list is what
    // made chip-hopping jank), and tapping a row only toggles local expansion
    // instead of round-tripping through the reveal's selectedChapter (which
    // used to reset sheet state + re-scroll on every tap).
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(chapter?.number, chapters.size) {
        if (chapter != null) {
            expandedNumber = chapter.number
        } else if (mode == BookNotesMode.CHAPTERS && !opened) {
            expandedNumber = chapters.firstOrNull()?.number
        }
        opened = true
        val idx = chapters.indexOfFirst { it.number == chapter?.number }
        if (idx >= 0) {
            listState.scrollToItem(idx + (if (hasSynopsis) 1 else 0))
        }
    }
    fun toggleChapter(ch: BookChapter) {
        expandedNumber = if (expandedNumber == ch.number) null else ch.number
    }
    fun toggleChapterRead(ch: BookChapter) {
        val chDone = chaptersDone >= ch.number
        AppPreferences.setBookReadingProgressExact(
            context,
            bookName,
            if (chDone) ch.number - 1 else ch.number
        )
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The notes sheets wear the CATEGORY-TINTED wash (same hue family as
        // the reveal page + cards); when the cover's palette is available it
        // replaces the category accent entirely (fallback stays when not).
        containerColor = coverPal?.container ?: cat.notesSheetContainerColor(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
                .fillMaxHeight(0.92f)
                .padding(bottom = 20.dp)
        ) {
            // ── Top hairline — soft accent rule under the drag handle ─────
            NotesSheetTopHairline(cat)
            Spacer(Modifier.height(10.dp))
            // ── Header — cover + title/author + heart + close ────────────
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                BookCoverPoster(
                    bookTitle = topic.name,
                    imageUrl = topic.imageUrl,
                    modifier = Modifier
                        .size(width = 76.dp, height = 114.dp)
                        .shadow(3.dp, RoundedCornerShape(8.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "BOOK NOTES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp
                        ),
                        color = ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        topic.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    topic.byline.takeIf { it.isNotBlank() }?.let { byline ->
                        Text(
                            byline,
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // v355 — the rating sits just below the author name: the
                    // fetched Google Books average AND the user's own rating
                    // together under the award-ribbon glyph (the old Reviews +
                    // Your-rating card is gone).
                    val fetchedRating = AppPreferences.bookRatingsState[topic.name]
                    val myRating = AppPreferences.bookCustomRatingsState[topic.name] ?: 0.0
                    if ((fetchedRating != null && fetchedRating > 0.0) || myRating > 0.0) {
                        Spacer(Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CurioIcon(
                                name = CurioIcons.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFF6B23B),
                                size = 13.dp
                            )
                            Text(
                                text = buildString {
                                    if (fetchedRating != null && fetchedRating > 0.0) {
                                        append(String.format("%.1f", fetchedRating))
                                        if (myRating > 0.0) append(" · yours ${myRating.toInt()} / 5")
                                    } else if (myRating > 0.0) {
                                        append("yours ${myRating.toInt()} / 5")
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVariant
                            )
                        }
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // v348 — book-level favorite heart (mirrors the album
                    // hearts in look and feel).
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            AppPreferences.toggleBookFavorite(context, bookName)
                        },
                        shape = CircleShape,
                        color = if (isFavBook) accent.copy(alpha = 0.2f) else surface.copy(alpha = 0.6f)
                    ) {
                        Box(
                            modifier = Modifier.padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HeartGlyph(
                                color = if (isFavBook) Color(0xFFE5484D) else onSurfaceVariant,
                                iconSize = 19.dp,
                                filled = isFavBook
                            )
                        }
                    }
                }
            }

            // ── Pinned reading-progress rail (v328) — stays above the list ─
            if (hasChapters) {
                Spacer(Modifier.height(12.dp))
                val progressLabel = if (chaptersDone > 0)
                    "$chaptersDone of ${chapters.size} chapters read"
                else "${chapters.size} chapters"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        progressLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "$chaptersDone / ${chapters.size}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = accent
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(surfaceHigh)
                ) {
                    val frac = if (chapters.size > 0)
                        (chaptersDone.toFloat() / chapters.size).coerceIn(0f, 1f) else 0f
                    if (frac > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(frac)
                                .height(4.dp)
                                .background(accent, RoundedCornerShape(50))
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── One scroll: synopsis accordion, then the chapter list ─────
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (hasSynopsis) {
                    item(key = "book_about") {
                        BookSynopsisAccordion(
                            surface = surface,
                            accent = accent,
                            ink = ink,
                            onSurface = onSurface,
                            synopsis = topic.synopsis.orEmpty(),
                            initiallyExpanded = !hasChapters
                        )
                    }
                }
                if (hasChapters) {
                    itemsIndexed(chapters) { _, ch ->
                        val isOpen = expandedNumber == ch.number
                        val isRead = ch.number <= chaptersDone
                        Surface(
                            onClick = { toggleChapter(ch) },
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isOpen -> accent
                                isRead -> surfaceAlt
                                else -> surface
                            },
                            // v354 — no elevation flip: the old 1dp lift popped
                            // in/out on expand (glitchy touch shadow). A read
                            // row gets a solid accent border instead.
                            shadowElevation = 0.dp,
                            border = if (isRead)
                                BorderStroke(1.dp, accent.copy(alpha = 0.55f))
                            else null
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    // Leading chip: the chapter number always
                                    // shows — a READ chapter tints the disc
                                    // softly in the accent (fill + number +
                                    // rim) instead of a loud ✓ (v355).
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isOpen -> onAccent
                                                    isRead -> accent.copy(alpha = 0.18f)
                                                    else -> surfaceHigh
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                when {
                                                    isOpen -> accent.copy(alpha = 0.5f)
                                                    isRead -> accent.copy(alpha = 0.55f)
                                                    else -> onSurfaceVariant.copy(alpha = 0.25f)
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${ch.number}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = when {
                                                isOpen -> accent
                                                isRead -> accent
                                                else -> onSurfaceVariant
                                            }
                                        )
                                    }
                                    Text(
                                        text = ch.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isOpen || isRead) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isOpen) onAccent else onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // v352 — per-row action chips: the Like
                                    // heart and the Mark-read toggle live on
                                    // the row (not inside the expanded panel),
                                    // so both are one tap away. Read state is
                                    // a SOLID accent fill (no more washed-out
                                    // read chips).
                                    val isLiked = ch.number in chapterLikes
                                    val chDone = chaptersDone >= ch.number
                                    Surface(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            AppPreferences.toggleBookChapterLike(context, bookName, ch.number)
                                        },
                                        shape = CircleShape,
                                        color = if (isLiked) Color(0xFFE5484D).copy(alpha = 0.18f)
                                                else surface.copy(alpha = 0.7f)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            HeartGlyph(
                                                color = if (isLiked) Color(0xFFE5484D)
                                                        else if (isOpen) onAccent.copy(alpha = 0.85f) else onSurfaceVariant,
                                                iconSize = 16.dp,
                                                filled = isLiked
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = { toggleChapterRead(ch) },
                                        shape = CircleShape,
                                        // Read = SOLID accent fill + rim in
                                        // EVERY state (the old open+read flip
                                        // to an onAccent disc read as a hole
                                        // punched in the accent row in dark
                                        // mode — v355).
                                        color = if (chDone)
                                            accent
                                            else surface.copy(alpha = 0.7f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (chDone)
                                                onAccent.copy(alpha = 0.7f)
                                                else onSurfaceVariant.copy(alpha = 0.25f)
                                        )
                                    ) {
                                        CurioIcon(
                                            CurioIcons.FoldedCorner,
                                            if (chDone) "Mark chapter unread" else "Mark chapter read",
                                            tint = if (chDone) onAccent else onSurfaceVariant,
                                            size = 16.dp,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }
                                // v354 — expand/collapse: height + fade together
                                // (the old fade-only pop read glitchy).
                                AnimatedVisibility(
                                    visible = isOpen,
                                    enter = expandVertically(
                                        animationSpec = tween(180)
                                    ) + fadeIn(
                                        animationSpec = tween(140)
                                    ),
                                    exit = shrinkVertically(
                                        animationSpec = tween(140)
                                    ) + fadeOut(
                                        animationSpec = tween(120)
                                    )
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(onAccent.copy(alpha = 0.18f))
                                        )
                                        if (ch.pageStart > 0 && ch.pageEnd > 0) {
                                            Text(
                                                "pp. ${ch.pageStart}–${ch.pageEnd}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isOpen) onAccent.copy(alpha = 0.9f) else ink
                                            )
                                        }
                                        Text(
                                            ch.summary.ifBlank { "No summary for this chapter." },
                                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                                            color = if (isOpen) onAccent else onSurface
                                        )
                                        // v352 — the Mark-read toggle + Like
                                        // heart moved OUT of the expanded panel
                                        // onto the row (chips above); the
                                        // panel now only holds pages + notes.
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * v348 — the book synopsis as a collapsible card pinned at the TOP of the
 * book-notes sheet (mirrors the album sheet's "About this album" accordion):
 * collapsed it shows a two-line teaser with a Read/Hide affordance, tapping
 * the card expands the full description or collapses it back.
 */
@Composable
private fun BookSynopsisAccordion(
    surface: Color,
    accent: Color,
    ink: Color,
    onSurface: Color,
    synopsis: String,
    initiallyExpanded: Boolean,
    modifier: Modifier = Modifier,
    // v350 — the series episode sheet reuses this accordion; the label swaps
    // to "ABOUT THIS SERIES" while the behaviour stays identical.
    label: String = "ABOUT THIS BOOK"
) {
    var expanded by remember(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = surface,
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.16f)
                ) {
                    CurioIcon(
                        CurioIcons.MenuBook,
                        null,
                        tint = ink,
                        size = 15.dp,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (expanded) "Hide" else "Read",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ink.copy(alpha = 0.9f)
                )
                CurioIcon(
                    if (expanded) CurioIcons.KeyboardArrowUp else CurioIcons.KeyboardArrowDown,
                    if (expanded) "Collapse synopsis" else "Expand synopsis",
                    tint = ink,
                    size = 20.dp
                )
            }
            Text(
                text = synopsis,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
// Album info section — track list + track chips (albums only)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Shared "top hairline" for the full-height NOTES sheets (book notes +
 * album track list): a soft accent rule under the drag handle giving the
 * category-tinted sheet a crisp accent top edge.
 */
@Composable
private fun NotesSheetTopHairline(cat: com.curio.app.data.CurioCategory) {
    val accent = cat.themedAccent()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0f),
                        accent.copy(alpha = if (isCurioDarkTheme()) 0.9f else 0.55f),
                        accent.copy(alpha = 0f)
                    )
                )
            )
    )
}

/**
 * Album info section shown below the hero for ALBUMS topics (mirrors the
 * book section). Displays:
 * - Album cover + a TRACKLIST card preview (tap → full sheet)
 * - Track chips for quick jumps into the track-list sheet at that track
 */
@Composable
private fun AlbumInfoSection(
    cat: com.curio.app.data.CurioCategory,
    topic: CurioTopic,
    onOpenSheet: () -> Unit,
    onTrackClick: (AlbumTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val tracks = topic.tracks.orEmpty()
    if (tracks.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AlbumTrackListCard(
            cat = cat,
            topic = topic,
            tracks = tracks,
            onClick = onOpenSheet
        )
        AlbumTrackChips(
            cat = cat,
            tracks = tracks,
            onTrackClick = onTrackClick
        )
    }
}

/**
 * TRACKLIST card — album artwork + a short track preview (mirrors the book
 * synopsis card). Tap opens the full-height track-list sheet.
 */
@Composable
private fun AlbumTrackListCard(
    cat: com.curio.app.data.CurioCategory,
    topic: CurioTopic,
    tracks: List<AlbumTrack>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cat.categorySurface(MaterialTheme.colorScheme.surface),
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row — icon + TRACKLIST + count + runtime
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    CurioIcon(
                        name = CurioIcons.Album,
                        contentDescription = null,
                        tint = cat.categoryInk(),
                        size = 16.dp,
                        modifier = Modifier.padding(7.dp)
                    )
                }
                Text(
                    text = "TRACKLIST".uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = cat.categoryInk()
                )
                Spacer(Modifier.weight(1f))
                val runtime = albumRuntimeSeconds(tracks)
                val meta = buildString {
                    append("${tracks.size} tracks")
                    if (runtime != null) append(" · ${formatAlbumRuntime(runtime)}")
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            // Artwork + a compact first-tracks preview.
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                AlbumCoverPoster(
                    albumTitle = topic.name,
                    artist = topic.byline,
                    accent = cat.themedAccent(),
                    imageUrl = topic.imageUrl,
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    topic.byline.takeIf { it.isNotBlank() }?.let { artist ->
                        Text(
                            artist,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Preview the first few tracks (mirror of the book
                    // synopsis preview — 5 lines max).
                    val preview = tracks.take(5)
                    preview.forEach { tr ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${tr.number}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = cat.categoryInk(),
                                maxLines = 1
                            )
                            Text(
                                text = tr.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (tr.duration.isNotBlank()) {
                                Text(
                                    text = tr.duration,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (tracks.size > 5) {
                        Text(
                            text = "+ ${tracks.size - 5} more tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (tracks.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "View the full track list →",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = cat.categoryInk(),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

/** Sum track durations (m:ss / h:mm:ss) into total seconds, or null when
 *  no track carries a parseable duration. */
private fun albumRuntimeSeconds(tracks: List<AlbumTrack>): Int? {
    var total = 0
    var any = false
    for (t in tracks) {
        val parts = t.duration.trim().split(":")
        val secs = when (parts.size) {
            2 -> (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
            3 -> (parts[0].toIntOrNull() ?: 0) * 3600 + (parts[1].toIntOrNull() ?: 0) * 60 + (parts[2].toIntOrNull() ?: 0)
            else -> 0
        }
        if (secs > 0) { total += secs; any = true }
    }
    return if (any) total else null
}

/** Format total seconds as "47 min" / "1 hr 02 min". */
private fun formatAlbumRuntime(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return if (h > 0) "$h hr ${m.toString().padStart(2, '0')} min" else "$m min"
}

/**
 * Album artwork poster for the reveal + track-list sheet. Albums carry no
 * authored imageUrl in the catalog, so the artwork is resolved on the fly
 * (iTunes Search first, MusicBrainz + Cover Art Archive fallback — both
 * keyless) by [AlbumArtFetch]. While resolving — or when nothing is found —
 * a tinted rounded tile with the Album glyph stands in.
 */
@Composable
private fun AlbumCoverPoster(
    albumTitle: String,
    artist: String,
    accent: Color,
    imageUrl: String? = null,
    modifier: Modifier = Modifier
) {
    // v333 — albums now ship an AUTHORED `imageUrl` in the catalog (iTunes /
    // MusicBrainz artwork), so the poster prefers it and only falls back to
    // the on-the-fly keyless resolver for albums without one (older rows).
    val authoredUrl = imageUrl?.takeIf { it.isNotBlank() }
    var artUrl by remember(albumTitle, artist, authoredUrl) { mutableStateOf<String?>(authoredUrl) }
    var failed by remember(albumTitle, artist, authoredUrl) { mutableStateOf(false) }
    // v350 — the ALBUM cover-fetch toggle now gates the keyless fallback
    // (authored art always shows; the network resolver only runs when the
    // album toggle is ON, mirroring the book + series consent gates).
    val consent = AppPreferences.albumFetchEnabledState
    LaunchedEffect(albumTitle, artist, authoredUrl, consent) {
        if (artUrl == null && !failed && authoredUrl == null && consent) {
            artUrl = AlbumArtFetch.resolveArtworkUrl(albumTitle, artist)
            if (artUrl == null) failed = true
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        lerp(accent, Color.White, if (isCurioDarkTheme()) 0.18f else 0.55f),
                        lerp(accent, Color.Black, 0.45f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(
            name = CurioIcons.Album,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (artUrl == null) 0.75f else 0f),
            size = 30.dp
        )
        val url = artUrl
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = "Album cover",
                contentScale = ContentScale.Crop,
                onError = {
                    failed = true
                    artUrl = null
                },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/** Horizontal track chips (mirror of the book chapter chips) — a tap opens
 *  the track-list sheet scrolled to that track. */
@Composable
private fun AlbumTrackChips(
    cat: com.curio.app.data.CurioCategory,
    tracks: List<AlbumTrack>,
    onTrackClick: (AlbumTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                CurioIcon(
                    name = CurioIcons.MusicNote,
                    contentDescription = null,
                    tint = cat.categoryInk(),
                    size = 16.dp,
                    modifier = Modifier.padding(7.dp)
                )
            }
            Text(
                text = "TRACKS".uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = cat.categoryInk()
            )
            Text(
                text = "${tracks.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            itemsIndexed(tracks) { _, track ->
                AlbumTrackChip(
                    cat = cat,
                    track = track,
                    onClick = { onTrackClick(track) }
                )
            }
        }
    }
}

/** One track chip — number + title + duration (compact, no summary). */
@Composable
private fun AlbumTrackChip(
    cat: com.curio.app.data.CurioCategory,
    track: AlbumTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow),
        shadowElevation = 2.dp,
        modifier = modifier
            .width(158.dp)
            .height(66.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "TR ${track.number}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = cat.categoryInk(),
                    maxLines = 1
                )
                if (track.duration.isNotBlank()) {
                    Text(
                        text = track.duration,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
            }
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Album track-list ModalBottomSheet — mirrors the book-notes sheet: album
 * artwork + title head the sheet, and the full track list (number/title/
 * duration) fills a tall scrollable body. A track chip on the reveal opens
 * it scrolled to (and highlighting) that track.
 *
 * v336 additions — all INSIDE the one sheet (no separate popups):
 *  - the album's synopsis as a collapsible "About this album" card pinned
 *    at the TOP of the track list (tap to expand/collapse),
 *  - a ♥ on every track row: multi-select favorite picks persisted per
 *    album (AppPreferences) that render as the Vinyl share card's
 *    FAVORITE TRACKS strip,
 *  - a LISTEN pill offering Apple Music / Spotify / YouTube Music / Amazon
 *    Music / Deezer that opens the service DIRECTLY (no explore session),
 *    plus the catalog-authored Genius pill.
 */
@Composable
private fun AlbumNotesSheet(
    cat: com.curio.app.data.CurioCategory,
    topic: CurioTopic,
    track: AlbumTrack?,
    onSelectTrack: (AlbumTrack) -> Unit,
    onDismiss: () -> Unit
) {
    val tracks = topic.tracks.orEmpty()
    if (tracks.isEmpty()) return
    val context = LocalContext.current
    // v339/v350 — cover-art palette: the sheet derives its FULL colour set
    // (background + cards + chips + text) from the ALBUM cover's artwork
    // (null swatches → per-element category fallback). v350 — the album
    // cover-fetch toggle gates the lookup like the book/series gates: when
    // OFF, only already-cached art is served, nothing touches the network.
    var coverSwatches by remember(topic.imageUrl) { mutableStateOf<CoverSwatches?>(null) }
    LaunchedEffect(topic.imageUrl) {
        coverSwatches = fetchCoverSwatches(
            context,
            topic.imageUrl,
            networkAllowed = AppPreferences.albumFetchEnabledState
        )
    }
    val coverPal = cat.notesSheetPalette(coverSwatches)
    val accent = coverPal?.accent ?: cat.themedAccent()
    val onAccent = coverPal?.onAccent ?: cat.onAccent()
    val ink = coverPal?.ink ?: cat.categoryInk()
    val surface = coverPal?.surface ?: cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow)
    val surfaceHigh = coverPal?.surfaceHigh ?: cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
    val surfaceAlt = coverPal?.surfaceAlt ?: MaterialTheme.colorScheme.secondaryContainer
    val onSurfaceAlt = coverPal?.onSurfaceAlt ?: MaterialTheme.colorScheme.onSecondaryContainer
    val onSurface = coverPal?.onSurface ?: MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = coverPal?.onSurfaceVariant ?: MaterialTheme.colorScheme.onSurfaceVariant
    // The opened track (from a reveal-section track chip) — null when the
    // sheet is opened from the TRACKLIST card, in which case no row is
    // pre-highlighted and the list starts at the top.
    val currentTrack = track
    // v336 — a collapsible "About this album" synopsis card sits at the top
    // of the track list when the catalog authors one; every scroll target
    // below shifts by one row so a chip-jump still lands on its track.
    val synopsis = topic.synopsis?.takeIf { it.isNotBlank() }
    val synopsisOffset = if (synopsis != null) 1 else 0
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Card-open (no pre-selected track) starts at the TOP (the synopsis card
    // when present); a chip-jump starts on that track, shifted past the
    // synopsis row.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (currentTrack != null)
            synopsisOffset + tracks.indexOfFirst { it.number == currentTrack.number }.coerceAtLeast(0)
        else 0
    )
    LaunchedEffect(currentTrack?.number) {
        if (currentTrack != null && tracks.size > 1) {
            listState.animateScrollToItem(
                synopsisOffset + tracks.indexOfFirst { it.number == currentTrack?.number }.coerceAtLeast(0)
            )
        }
    }
    val runtime = albumRuntimeSeconds(tracks)
    val haptics = LocalHapticFeedback.current
    // v357 — coroutine scope for the LISTEN pill's Apple Music album lookup
    // (deep-link resolution is a suspend iTunes search).
    val scope = rememberCoroutineScope()
    // v336 — heart-picked favorite tracks for this album (multi-select).
    // Read reactively so a row-heart tap updates every heart in the sheet
    // (and the Vinyl share card) without leaving it.
    val favTracks = AppPreferences.albumFavTracksState[topic.name].orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Same category-tinted wash + top hairline as the book-notes sheet.
        // v339 — when the cover's palette is available it replaces the
        // category accent (fallback stays when it isn't).
        containerColor = coverPal?.container ?: cat.notesSheetContainerColor(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
                .fillMaxHeight(0.92f)
                .padding(bottom = 20.dp)
        ) {
            // ── Top hairline — matches the book sheet ───────────────────
            NotesSheetTopHairline(cat)
            Spacer(Modifier.height(10.dp))

            // ── Header — artwork + album title/artist + close ──────────
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                AlbumCoverPoster(
                    albumTitle = topic.name,
                    artist = topic.byline,
                    accent = accent,
                    imageUrl = topic.imageUrl,
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(3.dp, RoundedCornerShape(12.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "ALBUM TRACKLIST",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp
                        ),
                        color = ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        topic.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    topic.byline.takeIf { it.isNotBlank() }?.let { byline ->
                        Text(
                            byline,
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    val meta = buildString {
                        append("${tracks.size} tracks")
                        if (runtime != null) append(" · ${formatAlbumRuntime(runtime)}")
                    }
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ink
                    )
                }
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = surface.copy(alpha = 0.6f)
                ) {
                    CurioIcon(
                        CurioIcons.Close,
                        "Close track list",
                        tint = onSurfaceVariant,
                        size = 20.dp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // ── v336 — Listen actions row: a LISTEN pill (always present)
            // offering Apple Music / Spotify / YouTube Music / Amazon Music /
            // Deezer straight from the sheet (no explore session), plus the
            // catalog-authored Genius pill when a geniusUrl exists.
            Spacer(Modifier.height(12.dp))
            var listenMenuOpen by remember { mutableStateOf(false) }
            val geniusUrl = topic.geniusUrl?.takeIf { it.isNotBlank() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Box {
                    Surface(
                        onClick = { listenMenuOpen = true },
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.14f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            CurioIcon(
                                CurioIcons.MusicNote,
                                "Listen to this album",
                                tint = onAccent,
                                size = 16.dp
                            )
                            Text(
                                "LISTEN",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = ink,
                                maxLines = 1
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = listenMenuOpen,
                        onDismissRequest = { listenMenuOpen = false }
                    ) {
                        albumListenServices.forEach { srv ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        srv.label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                },
                                onClick = {
                                    listenMenuOpen = false
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    // v357/v358 — Apple Music and Spotify open
                                    // the REAL album: an iTunes / Spotify API
                                    // lookup resolves the album page (the same
                                    // native deep link songs use), falling back
                                    // to a search on a miss. Spotify needs the
                                    // optional client id+secret; the remaining
                                    // services have no keyless album-ID lookup
                                    // and keep the search link.
                                    if (srv.id == "apple" || srv.id == "spotify") {
                                        scope.launch {
                                            val deep = if (srv.id == "apple")
                                                resolveAppleMusicItemUrl(topic)
                                            else
                                                resolveSpotifyItemUrl(topic)
                                            val url = deep ?: albumListenUrl(topic, srv.id)
                                            if (url.isNotBlank()) openSearchUrl(context, url)
                                        }
                                    } else {
                                        val url = albumListenUrl(topic, srv.id)
                                        if (url.isNotBlank()) openSearchUrl(context, url)
                                    }
                                },
                                leadingIcon = {
                                    CurioIcon(
                                        srv.icon,
                                        "Open in ${srv.label}",
                                        tint = accent,
                                        size = 18.dp
                                    )
                                }
                            )
                        }
                    }
                }
                if (geniusUrl != null) {
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            openSearchUrl(context, geniusUrl)
                        },
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.14f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                        ) {
                            CurioIcon(
                                CurioIcons.OpenInNew,
                                "Open album on Genius",
                                tint = onAccent,
                                size = 16.dp
                            )
                            Text(
                                "GENIUS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = ink,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // ── Full track list — scrollable, selected track highlighted ─
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (synopsis != null) {
                    item(key = "album_about") {
                        AlbumSynopsisAccordion(
                            surface = surface,
                            accent = accent,
                            ink = ink,
                            onSurface = onSurface,
                            synopsis = synopsis
                        )
                    }
                }
                itemsIndexed(tracks) { _, tr ->
                    val selected = tr.number == currentTrack?.number
                    Surface(
                        onClick = { onSelectTrack(tr) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) accent
                                else surface,
                        shadowElevation = if (selected) 0.dp else 1.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "${tr.number}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) onAccent else ink,
                                maxLines = 1,
                                modifier = Modifier.width(28.dp)
                            )
                            Text(
                                text = tr.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (selected) onAccent else onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (tr.duration.isNotBlank()) {
                                Text(
                                    text = tr.duration,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) onAccent.copy(alpha = 0.85f)
                                            else onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            // v336 — favorite heart: taps add/remove this track
                            // from the album's share-card favorites (multi-
                            // select; the Vinyl card renders the whole strip).
                            val fav = favTracks.contains(tr.title)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        AppPreferences.toggleAlbumFavoriteTrack(context, topic.name, tr.title)
                                    }
                            ) {
                                HeartGlyph(
                                    color = if (fav) Color(0xFFE5484D)
                                            else if (selected) onAccent.copy(alpha = 0.85f)
                                            else onSurfaceVariant.copy(alpha = 0.6f),
                                    iconSize = 18.dp,
                                    filled = fav,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// v336 — Album sheet helpers (synopsis accordion, heart glyph, listen links)
// ═══════════════════════════════════════════════════════════════════════════

/** The services offered by the album sheet's LISTEN pill (v336) — search
 *  deep links that open the installed app (or the browser) DIRECTLY,
 *  bypassing the explore session entirely. Extensible later. */
private data class AlbumListenService(val id: String, val label: String, val icon: String)

private val albumListenServices = listOf(
    AlbumListenService("apple", "Apple Music", CurioIcons.MusicNote),
    AlbumListenService("spotify", "Spotify", CurioIcons.PlayCircle),
    AlbumListenService("ytm", "YouTube Music", CurioIcons.YouTubeActivity),
    AlbumListenService("amazon", "Amazon Music", "radio"),
    AlbumListenService("deezer", "Deezer", CurioIcons.Album)
)

/** v336 — the search deep link for [service] on the album sheet. Artist +
 *  album title (trailing "(1966)" year stripped — search engines rank a
 *  bare title higher), then the same scheme tricks [openSearchUrl] already
 *  handles: Apple Music via `music://` (falls back to https when the app
 *  isn't installed), Spotify/Amazon/Deezer via their web search routes,
 *  YouTube Music pinned to the app package by [openSearchUrl]. */
// ═══════════════════════════════════════════════════════════════════════════
// Series info section — poster + episode list (series only)
// ═══════════════════════════════════════════════════════════════════════════

/** Series section on the reveal: a poster card with the synopsis preview and
 *  episode count; tapping the card opens the full episode-list sheet (v350,
 *  mirrors the album track-list card). */
@Composable
private fun SeriesInfoSection(
    cat: com.curio.app.data.CurioCategory,
    topic: CurioTopic,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val episodes = topic.episodes.orEmpty()
    if (episodes.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SeriesPosterCard(
            cat = cat,
            topic = topic,
            episodes = episodes,
            onClick = onOpenSheet
        )
    }
}

/** EPISODES card — poster + synopsis preview + season/episode meta. Tap opens
 *  the full-height episode-list sheet (mirrors the album track-list card). */
@Composable
private fun SeriesPosterCard(
    cat: com.curio.app.data.CurioCategory,
    topic: CurioTopic,
    episodes: List<com.curio.app.data.SeriesEpisode>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seasonCount = episodes.map { it.season }.distinct().size
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cat.categorySurface(MaterialTheme.colorScheme.surface),
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row — icon + EPISODES + count + season meta
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    CurioIcon(
                        name = CurioIcons.Movies,
                        contentDescription = null,
                        tint = cat.categoryInk(),
                        size = 16.dp,
                        modifier = Modifier.padding(7.dp)
                    )
                }
                Text(
                    text = "EPISODES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = cat.categoryInk()
                )
                Spacer(Modifier.weight(1f))
                val meta = buildString {
                    append("${episodes.size} episode")
                    if (episodes.size != 1) append("s")
                    if (seasonCount > 1) append(" · $seasonCount seasons")
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            // Poster + synopsis preview (or a short episode-title preview).
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                SeriesPoster(
                    showName = topic.name,
                    accent = cat.themedAccent(),
                    imageUrl = topic.imageUrl,
                    modifier = Modifier
                        .size(width = 76.dp, height = 112.dp)
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    topic.byline.takeIf { it.isNotBlank() }?.let { creator ->
                        Text(
                            creator,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val synopsis = topic.synopsis?.takeIf { it.isNotBlank() }
                    if (synopsis != null) {
                        Text(
                            text = synopsis,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        episodes.take(4).forEach { ep ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = ep.key(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = cat.categoryInk(),
                                    maxLines = 1
                                )
                                Text(
                                    text = ep.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
            if (episodes.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "View the episode list →",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = cat.categoryInk(),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

/** Keyed "S1E3" for an episode — matches the watched-progress store keys. */
private fun com.curio.app.data.SeriesEpisode.key(): String = "S${season}E${number}"

/** Series poster tile: authored imageUrl first, then the keyless TVMaze /
 *  iTunes resolver (only when the SERIES cover-fetch toggle is ON), and a
 *  tinted gradient tile with the Movies glyph while resolving / on miss. */
@Composable
private fun SeriesPoster(
    showName: String,
    accent: Color,
    imageUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val authoredUrl = imageUrl?.takeIf { it.isNotBlank() }
    var artUrl by remember(showName, authoredUrl) { mutableStateOf<String?>(authoredUrl) }
    var failed by remember(showName, authoredUrl) { mutableStateOf(false) }
    // v350 — the keyless fallback only runs when the SERIES fetch toggle is on.
    val consent = AppPreferences.seriesFetchEnabledState
    LaunchedEffect(showName, authoredUrl, consent) {
        if (artUrl == null && !failed && authoredUrl == null && consent) {
            artUrl = SeriesPosterFetch.resolvePosterUrl(showName)
            if (artUrl == null) failed = true
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        lerp(accent, Color.White, if (isCurioDarkTheme()) 0.18f else 0.55f),
                        lerp(accent, Color.Black, 0.45f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        CurioIcon(
            name = CurioIcons.Movies,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (artUrl == null) 0.75f else 0f),
            size = 30.dp
        )
        val url = artUrl
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * v350 — the full-height EPISODE LIST sheet for a series topic. One
 * album-style scroll (mirrors the book-notes / album-track-list sheets): a
 * header with the poster + title + favorite heart, a pinned watched-progress
 * rail, a collapsible "About this series" synopsis accordion at the top, and
 * the episodes grouped by season as expandable rows (summary + watched
 * toggle). The whole sheet wears the poster's extracted palette when the
 * series cover-fetch toggle is on; otherwise the category tint.
 */
@Composable
private fun EpisodeNotesSheet(
    cat: com.curio.app.data.CurioCategory,
    topic: CurioTopic,
    onDismiss: () -> Unit
) {
    val episodes = topic.episodes.orEmpty()
    if (episodes.isEmpty()) return
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val fetchConsent = AppPreferences.seriesFetchEnabledState
    var coverSwatches by remember(topic.imageUrl) { mutableStateOf<CoverSwatches?>(null) }
    LaunchedEffect(topic.imageUrl, fetchConsent) {
        coverSwatches = fetchCoverSwatches(context, topic.imageUrl, networkAllowed = fetchConsent)
    }
    val coverPal = cat.notesSheetPalette(coverSwatches)
    val accent = coverPal?.accent ?: cat.themedAccent()
    val onAccent = coverPal?.onAccent ?: cat.onAccent()
    val ink = coverPal?.ink ?: cat.categoryInk()
    val surface = coverPal?.surface ?: cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow)
    val surfaceHigh = coverPal?.surfaceHigh ?: cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
    val surfaceAlt = coverPal?.surfaceAlt ?: MaterialTheme.colorScheme.secondaryContainer
    val onSurfaceAlt = coverPal?.onSurfaceAlt ?: MaterialTheme.colorScheme.onSecondaryContainer
    val onSurface = coverPal?.onSurface ?: MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = coverPal?.onSurfaceVariant ?: MaterialTheme.colorScheme.onSurfaceVariant
    val showName = topic.name
    val hasSynopsis = !topic.synopsis.isNullOrBlank()
    val seasons = episodes.map { it.season }.distinct().sorted()
    // v350 — favorite heart for the whole series (like the book heart).
    val isFavSeries = showName in AppPreferences.seriesFavoritesState
    // Watched progress: derived from the watched "S1E3" keys against the
    // authored episode list (so grouped/season data counts real episodes).
    val watchedKeys = AppPreferences.seriesWatchedState[showName].orEmpty()
    // v352 — per-episode Like hearts (show name → liked "S1E3" keys).
    val episodeLikes = AppPreferences.seriesEpisodeLikesState[showName].orEmpty()
    val watchedTotal = episodes.count { it.key() in watchedKeys }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    fun toggleEpisode(ep: com.curio.app.data.SeriesEpisode) {
        expandedKey = if (expandedKey == ep.key()) null else ep.key()
    }
    fun toggleWatched(ep: com.curio.app.data.SeriesEpisode) {
        AppPreferences.toggleSeriesEpisodeWatched(context, showName, ep.key())
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = coverPal?.container ?: cat.notesSheetContainerColor(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
                .fillMaxHeight(0.92f)
                .padding(bottom = 20.dp)
        ) {
            // ── Top hairline — soft accent rule under the drag handle ─────
            NotesSheetTopHairline(cat)
            Spacer(Modifier.height(10.dp))
            // ── Header — poster + title/creator + heart + close ──────────
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                SeriesPoster(
                    showName = showName,
                    accent = accent,
                    imageUrl = topic.imageUrl,
                    modifier = Modifier
                        .size(width = 76.dp, height = 112.dp)
                        .shadow(3.dp, RoundedCornerShape(10.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "SERIES NOTES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp
                        ),
                        color = ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        topic.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    topic.byline.takeIf { it.isNotBlank() }?.let { creator ->
                        Text(
                            "Created by $creator",
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // v350 — series-level favorite heart (mirrors the book
                    // and album hearts in look and feel).
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            AppPreferences.toggleSeriesFavorite(context, showName)
                        },
                        shape = CircleShape,
                        color = if (isFavSeries) accent.copy(alpha = 0.2f) else surface.copy(alpha = 0.6f)
                    ) {
                        Box(
                            modifier = Modifier.padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HeartGlyph(
                                color = if (isFavSeries) Color(0xFFE5484D) else onSurfaceVariant,
                                iconSize = 19.dp,
                                filled = isFavSeries
                            )
                        }
                    }
                }
            }

            // ── Pinned watched-progress rail — stays above the list ──────
            Spacer(Modifier.height(12.dp))
            val progressLabel = if (watchedTotal > 0)
                "$watchedTotal of ${episodes.size} episodes watched"
            else "${episodes.size} episodes"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    progressLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$watchedTotal / ${episodes.size}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = accent
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(surfaceHigh)
            ) {
                val frac = (watchedTotal.toFloat() / episodes.size).coerceIn(0f, 1f)
                if (frac > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(frac)
                            .height(4.dp)
                            .background(accent, RoundedCornerShape(50))
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── One scroll: synopsis accordion, then seasons + episodes ──
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (hasSynopsis) {
                    item(key = "series_about") {
                        BookSynopsisAccordion(
                            surface = surface,
                            accent = accent,
                            ink = ink,
                            onSurface = onSurface,
                            synopsis = topic.synopsis.orEmpty(),
                            initiallyExpanded = !hasSynopsis || episodes.isEmpty(),
                            label = "ABOUT THIS SERIES"
                        )
                    }
                }
                seasons.forEach { season ->
                    val seasonEps = episodes.filter { it.season == season }
                    val watchedSeason = seasonEps.count { it.key() in watchedKeys }
                    item(key = "season_$season") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 2.dp, top = 8.dp, end = 2.dp, bottom = 2.dp)
                        ) {
                            Text(
                                "SEASON $season",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = ink
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "$watchedSeason / ${seasonEps.size} watched",
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVariant
                            )
                        }
                    }
                    itemsIndexed(seasonEps) { _, ep ->
                        val isOpen = expandedKey == ep.key()
                        val isWatched = ep.key() in watchedKeys
                        Surface(
                            onClick = { toggleEpisode(ep) },
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isOpen -> accent
                                isWatched -> surfaceAlt
                                else -> surface
                            },
                            // v354 — no elevation flip (glitchy touch shadow);
                            // watched rows get a solid accent border instead.
                            shadowElevation = 0.dp,
                            border = if (isWatched)
                                BorderStroke(1.dp, accent.copy(alpha = 0.55f))
                            else null
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    // Leading chip: the episode number always
                                    // shows — a WATCHED episode tints the disc
                                    // softly in the accent (fill + number +
                                    // rim) instead of a loud ✓ (v355, mirrors
                                    // the book sheet).
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isOpen -> onAccent
                                                    isWatched -> accent.copy(alpha = 0.18f)
                                                    else -> surfaceHigh
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                when {
                                                    isOpen -> accent.copy(alpha = 0.5f)
                                                    isWatched -> accent.copy(alpha = 0.55f)
                                                    else -> onSurfaceVariant.copy(alpha = 0.25f)
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${ep.number}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = when {
                                                isOpen -> accent
                                                isWatched -> accent
                                                else -> onSurfaceVariant
                                            }
                                        )
                                    }
                                    Text(
                                        text = ep.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isOpen || isWatched) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isOpen) onAccent else onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // v352 — per-row action chips (mirroring
                                    // the book sheet): the Like heart and the
                                    // Watched toggle live on the row; watched
                                    // gets a SOLID accent fill.
                                    val isLiked = ep.key() in episodeLikes
                                    Surface(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            AppPreferences.toggleSeriesEpisodeLike(context, showName, ep.key())
                                        },
                                        shape = CircleShape,
                                        color = if (isLiked) Color(0xFFE5484D).copy(alpha = 0.18f)
                                                else surface.copy(alpha = 0.7f)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            HeartGlyph(
                                                color = if (isLiked) Color(0xFFE5484D)
                                                        else if (isOpen) onAccent.copy(alpha = 0.85f) else onSurfaceVariant,
                                                iconSize = 16.dp,
                                                filled = isLiked
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = { toggleWatched(ep) },
                                        shape = CircleShape,
                                        // Watched = SOLID accent fill + rim in
                                        // EVERY state (the old open+watched
                                        // flip to an onAccent disc read as a
                                        // hole in the accent row in dark mode
                                        // — v355, mirrors the book sheet).
                                        color = if (isWatched)
                                            accent
                                            else surface.copy(alpha = 0.7f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isWatched)
                                                onAccent.copy(alpha = 0.7f)
                                                else onSurfaceVariant.copy(alpha = 0.25f)
                                        )
                                    ) {
                                        CurioIcon(
                                            CurioIcons.FoldedCorner,
                                            if (isWatched) "Mark episode unwatched" else "Mark episode watched",
                                            tint = if (isWatched) onAccent else onSurfaceVariant,
                                            size = 16.dp,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }
                                // v354 — expand/collapse: height + fade together
                                // (the old fade-only pop read glitchy).
                                AnimatedVisibility(
                                    visible = isOpen,
                                    enter = expandVertically(
                                        animationSpec = tween(180)
                                    ) + fadeIn(
                                        animationSpec = tween(140)
                                    ),
                                    exit = shrinkVertically(
                                        animationSpec = tween(140)
                                    ) + fadeOut(
                                        animationSpec = tween(120)
                                    )
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(onAccent.copy(alpha = 0.18f))
                                        )
                                        Text(
                                            "Season $season · Episode ${ep.number}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isOpen) onAccent.copy(alpha = 0.9f) else ink
                                        )
                                        Text(
                                            ep.summary.ifBlank { "No summary for this episode." },
                                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                                            color = if (isOpen) onAccent else onSurface
                                        )
                                        // v352 — the Watched toggle + Like
                                        // heart moved onto the row (chips
                                        // above); the panel now only holds
                                        // the episode notes.
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun albumListenUrl(topic: CurioTopic, service: String): String {
    val title = topic.name.replace(Regex("""\s+\(\d{4}\)\s*$"""), "")
    val q = Uri.encode(listOfNotNull(
        topic.byline.takeIf { it.isNotBlank() },
        title
    ).joinToString(" "))
    return when (service) {
        "apple" -> "music://music.apple.com/search?term=$q"
        "spotify" -> "https://open.spotify.com/search/$q"
        "ytm" -> "https://music.youtube.com/search?q=$q"
        "amazon" -> "https://music.amazon.com/search/$q"
        else -> "https://www.deezer.com/search/$q"
    }
}

/**
 * v336 — the album synopsis as a collapsible card pinned at the TOP of the
 * track-list sheet (one sheet — no separate popup). Collapsed it shows a
 * two-line teaser with a chevron; tapping the card expands the full
 * description or collapses it back.
 */
@Composable
private fun AlbumSynopsisAccordion(
    surface: Color,
    accent: Color,
    ink: Color,
    onSurface: Color,
    synopsis: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = surface,
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.16f)
                ) {
                    CurioIcon(
                        CurioIcons.MusicNote,
                        null,
                        tint = ink,
                        size = 15.dp,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Text(
                    "ABOUT THIS ALBUM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (expanded) "Hide" else "Read",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ink.copy(alpha = 0.9f)
                )
                CurioIcon(
                    if (expanded) CurioIcons.KeyboardArrowUp else CurioIcons.KeyboardArrowDown,
                    if (expanded) "Collapse synopsis" else "Expand synopsis",
                    tint = ink,
                    size = 20.dp
                )
            }
            Text(
                text = synopsis,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

/**
 * v336 — a tiny heart drawn directly (the bundled Material Symbols subset
 * has no "favorite" ligature, so a Canvas heart can never render as tofu,
 * and it exports identically through the share-card software pipeline).
 * [iconSize] avoids shadowing DrawScope.size.
 */
@Composable
private fun HeartGlyph(
    color: Color,
    iconSize: Dp,
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.size(iconSize)) {
        val w = this.size.width
        val h = this.size.height
        val heart = Path().apply {
            moveTo(w * 0.50f, h * 0.30f)
            cubicTo(w * 0.50f, h * 0.21f, w * 0.44f, h * 0.14f, w * 0.33f, h * 0.14f)
            cubicTo(w * 0.17f, h * 0.14f, w * 0.10f, h * 0.25f, w * 0.10f, h * 0.36f)
            cubicTo(w * 0.10f, h * 0.52f, w * 0.24f, h * 0.65f, w * 0.50f, h * 0.92f)
            cubicTo(w * 0.76f, h * 0.65f, w * 0.90f, h * 0.52f, w * 0.90f, h * 0.36f)
            cubicTo(w * 0.90f, h * 0.25f, w * 0.83f, h * 0.14f, w * 0.67f, h * 0.14f)
            cubicTo(w * 0.56f, h * 0.14f, w * 0.50f, h * 0.21f, w * 0.50f, h * 0.30f)
            close()
        }
        if (filled) {
            drawPath(heart, color)
        } else {
            drawPath(heart, color, style = Stroke(width = w * 0.10f))
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
// Tags row — directly below the hero (v132, restored to the scroll body)
// ═══════════════════════════════════════════════════════════════════════════

/** Tag chips directly under the hero card — v135: ALL of the topic's tags
 *  (no more 3-chip cap) plus a derived decade chip ("1940s") when a year is
 *  recoverable from the name/teaser. FlowRow wraps to multiple rows so a
 *  long tag set never runs off-screen. Same chip recipe the bottom band
 *  used: an opaque accent-tinted surface with a 2dp lift. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsRow(
    cat: com.curio.app.data.CurioCategory,
    resolved: CurioTopic?,
    modifier: Modifier = Modifier
) {
    val tags = resolved?.tags.orEmpty()
    val decade = resolved?.derivedDecadeTag()
    if (tags.isEmpty() && decade == null) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            RevealTagChip(
                text = tag,
                fill = lerp(MaterialTheme.colorScheme.surface, cat.themedAccent(), 0.32f)
            )
        }
        if (decade != null) {
            RevealTagChip(
                text = decade,
                fill = lerp(MaterialTheme.colorScheme.surface, cat.themedAccent(), 0.32f)
            )
        }
    }
}

/** One tag chip inside [TagsRow] — opaque accent-tinted surface, 2dp lift. */
@Composable
private fun RevealTagChip(
    text: String,
    fill: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = fill,
        shadowElevation = 2.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
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
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            // v28 — dark mode elevation visibility (glow + hairline).
            .curioDarkGlow(3.dp, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // v35 — the curiosity glyph in a small accent-tinted tile,
                // matching the ActionPromptCard's icon-tile language.
                Surface(
                    shape = CircleShape,
                    color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    CurioIcon(
                        name = CurioIcons.Lightbulb,
                        contentDescription = null,
                        tint = cat.categoryInk(),
                        size = 16.dp,
                        modifier = Modifier.padding(7.dp)
                    )
                }
                // v35 — eyebrow: a small-caps kicker. The old titleSmall
                // label was SMALLER than the fact body below it (inverted
                // hierarchy); the caps kicker reads as a label above the
                // editorial serif body.
                Text(
                    text = "One quirky fact to get you curious".uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = cat.categoryInk(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            // v43 — the quick fact reads in the Lora editorial serif so it
            // matches the instruction paragraph below (ONE readable font for
            // the reveal's long-form copy instead of the old sans/serif mix).
            // v49 — the fact and the instruction share [RevealEditorialBody]
            // (15sp — a notch smaller than the old 17sp fact). Shown IN FULL
            // — no line clamp, no read-more folding.
            Text(
                text = teaser ?: "Loading topic…",
                style = RevealEditorialBody,
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
    modifier: Modifier = Modifier,
    // v221 — for QUOTES, the full quote replaces the instruction text.
    instructionOverride: String? = null
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow),
        shadowElevation = 3.dp,
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
                // v37 — the trailing arrow affordance is gone: the card's
                // icon tile + bold title already read as actionable, and the
                // arrow just crowded the subtype line.
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = instructionOverride ?: action.instruction,
                // v35/v49 — the instruction reads in the Lora editorial
                // serif; since v49 it IS the shared [RevealEditorialBody] —
                // the exact same 15sp/23sp style as the quick fact above.
                style = RevealEditorialBody,
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

/** The reveal's floating Favorite capsule — a single raised pill with
 *  a star icon. When tapped, the topic is marked as a favorite (liked).
 *  The active state fills with the category accent; inactive is transparent.
 *  Animates like the nav bar's expand-on-active pill. */
@Composable
private fun RevealSentimentPill(
    sentiment: String?,
    accent: Color,
    ink: Color,
    container: Color,
    onFavorite: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val isFav = sentiment == AppPreferences.SENTIMENT_LIKE
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = container,
            shadowElevation = 6.dp
        ) {
            SentimentSegment(
                icon = if (isFav) CurioIcons.Star else CurioIcons.StarOutline,
                label = "Favorite",
                active = isFav,
                accent = accent,
                ink = ink,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFavorite()
                },
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

// v149 — sentiment segment geometry, mirroring the nav bar's pill sizes.
// v154 — bumped to EXACTLY the nav bar's sizes (60dp/128dp/60dp + 26dp
// icon) so the reveal Like/Dislike pill matches the bigger bottom pill.
// v159 — height slimmed 60 → 48dp WITH the nav bar (lengths unchanged).
// v201 — bumped to the nav bar's CURRENT sizes (64/136dp + 52dp height,
// the v184 sizing) so the Like/Dislike pill matches the bar again.
private val RevealSentimentIconWidth = 64.dp
private val RevealSentimentExpandedWidth = 136.dp
private val RevealSentimentHeight = 52.dp

// v162 — same ONE-spring-family fix as the nav bar: width, fill, icon tint
// and the label expand/shrink all run identical spring params so the
// segment moves in lockstep (before, the fill lagged on MediumLow and the
// label/icon finished early on their own tweens). v165 — specs typed per
// animated value (spring<Color> for colors, spring<IntSize> for the
// label's expand/shrink, spring<Float> for fades); same physics.
// v166 — mirrors the nav-pill family: slower (750 vs Medium 1500) and
// critically damped (1.0) so the Like/Dislike segments glide with zero
// overshoot — same feel as the bottom bar's collapse. v173 — slowed to
// 400 with the nav pill family ("still too rapid"). v201 — slowed to 150
// with the nav pill family ("smoother"), the calmest glide yet. v206 —
// 120 with the nav family ("even smoother").
private val RevealWidthSpring = spring<Dp>(dampingRatio = 1f, stiffness = 120f)
private val RevealMotionSpring = spring<Float>(dampingRatio = 1f, stiffness = 120f)
private val RevealColorSpring = spring<Color>(dampingRatio = 1f, stiffness = 120f)
private val RevealExpandSpring = spring<IntSize>(dampingRatio = 1f, stiffness = 120f)

/** One segment inside [RevealSentimentPill] — v149: mirrors the floating
 *  nav bar's expand-on-active pill: icons at rest (60dp), the ACTIVE
 *  segment springs wider and slides its label out (same spring + label
 *  slide-out, exit instant), the deselected one collapses back to its
 *  icon. Active fill = the category accent (v27q solid-selection) +
 *  on-accent ink. */
@Composable
private fun SentimentSegment(
    icon: String,
    label: String,
    active: Boolean,
    accent: Color,
    ink: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillWidth by animateDpAsState(
        targetValue = if (active) RevealSentimentExpandedWidth else RevealSentimentIconWidth,
        // v162 — shared [RevealWidthSpring], identical to the fill / icon /
        // label springs so the segment stays in lockstep.
        animationSpec = RevealWidthSpring,
        label = "revealSentimentWidth"
    )
    // v162 — fill + icon tint fade on the SAME spring as the width (before:
    // fill lagged on MediumLow, icon finished in 200ms — out of step).
    // v166 — the fill is CALMED like the nav pill (light mode pulls
    // saturation ~45% so the bright accent reads muted, not neon; dark +
    // pastel modes keep their already-muted tones).
    val fill = if (!isCurioDarkTheme() && !AppPreferences.pastelColorsState) {
        val a = toHsl(accent)
        fromHsl(a.h, (a.s * 0.55f).coerceAtMost(0.55f), a.l)
    } else accent
    val fillColor by animateColorAsState(
        targetValue = fill.copy(alpha = if (active) 1f else 0f),
        animationSpec = RevealColorSpring,
        label = "revealSentimentFill"
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) ink else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = RevealColorSpring,
        label = "revealSentimentIconTint"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = fillColor,
        modifier = modifier
            .width(pillWidth)
            .height(RevealSentimentHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CurioIcon(
                name = icon,
                contentDescription = label,
                tint = iconTint,
                size = 26.dp
            )
            // v162 — the label expands/shrinks + fades on the SAME spring
            // as the segment width (its own tweens used to finish ~3x early,
            // so the label was done while the segment was still mid-flight).
            AnimatedVisibility(
                visible = active,
                enter = expandHorizontally(RevealExpandSpring, expandFrom = Alignment.Start) + fadeIn(RevealMotionSpring),
                exit = shrinkHorizontally(RevealExpandSpring, shrinkTowards = Alignment.Start) + fadeOut(RevealMotionSpring)
            ) {
                Text(
                    text = label,
                    // v201 — EXACTLY the nav bar's label: Changa One display
                    // face, 15sp, Normal (was labelMedium Bold, which read
                    // thinner and smaller than the bar's tab labels).
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = ChangaOneFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp
                    ),
                    color = ink,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp, end = 2.dp)
                )
            }
        }
    }
}/** The reveal's floating Category + Favorite bar (v212) — category icon
 *  + name on the left (auto-expands on entry to show name, collapses
 *  when the bar exits), favorite star on the right. Wears the page's
 *  dynamic tint. */
@Composable
private fun RevealCategoryFavoriteBar(
    cat: CurioCategory,
    isFavorited: Boolean,
    accent: Color,
    ink: Color,
    container: Color,
    // v292 - opens the topic share-card sheet.
    onShare: () -> Unit = {},
    onFavorite: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    // Category pill: auto-expands from icon-only to showing name on entry.
    var categoryExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { categoryExpanded = true }
    // v223 — the expanded width now FITS the category name: measure the
    // name at the exact label style and size the pill to icon + paddings
    // + text (+ a little slack), instead of the old fixed 200dp that
    // always expanded to the max no matter how short or long the name.
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val categoryNameStyle = MaterialTheme.typography.labelMedium.copy(
        fontFamily = ChangaOneFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    )
    val categoryNamePx = remember(cat.displayName) {
        textMeasurer.measure(AnnotatedString(cat.displayName), style = categoryNameStyle)
            .size.width
    }
    val expandedCategoryWidth = with(density) {
        (24.dp.toPx() + 6.dp.toPx() + 2.dp.toPx() + categoryNamePx.toFloat() + 14.dp.toPx())
            .toDp()
    }
    val categoryWidth by animateDpAsState(
        targetValue = if (categoryExpanded) expandedCategoryWidth else 56.dp,
        animationSpec = RevealWidthSpring,
        label = "categoryBarWidth"
    )
    val categoryFill by animateColorAsState(
        targetValue = accent,
        animationSpec = RevealColorSpring,
        label = "categoryBarFill"
    )
    val categoryInk by animateColorAsState(
        targetValue = ink,
        animationSpec = RevealColorSpring,
        label = "categoryBarInk"
    )
    // Favorite star: icon-only at rest, expands to show label when
    // favorited. v223 — it now plays the SAME entry animation as the
    // category pill: starts collapsed on entry and springs open when the
    // topic is ALREADY favorited (before, opening a favorited topic just
    // sat there fully expanded with no animation).
    var favoriteRevealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { favoriteRevealed = true }
    val favoriteShown = isFavorited && favoriteRevealed
    val favWidth by animateDpAsState(
        targetValue = if (favoriteShown) RevealSentimentExpandedWidth else RevealSentimentIconWidth,
        animationSpec = RevealWidthSpring,
        label = "favBarWidth"
    )
    val favIconTint by animateColorAsState(
        targetValue = if (isFavorited) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = RevealColorSpring,
        label = "favIconTint"
    )
    val favFill by animateColorAsState(
        targetValue = if (isFavorited) accent else Color.Transparent,
        animationSpec = RevealColorSpring,
        label = "favFill"
    )
    val favLabelInk by animateColorAsState(
        targetValue = if (isFavorited) ink else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = RevealColorSpring,
        label = "favLabelInk"
    )
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        // v227 — liquid-glass experiment: refracted backdrop instead of
        // the solid elevated fill when the toggle is on. v243 — gated on
        // REQUESTED now: pre-Android-12 devices get the simulated glass
        // recipe from inside [liquidGlassCapsule].
        val glassOn = isLiquidGlassRequested()
        Surface(
            shape = RoundedCornerShape(50),
            color = if (glassOn) Color.Transparent else container,
            shadowElevation = if (glassOn) 0.dp else 6.dp,
            // v272 - RESTORE real refraction: this pill composes through
            // [SentimentPillHost] as an overlay SIBLING of the NavHost's
            // captured pages Box (same geometry as the bottom nav bar), so
            // sampling the global capture can never self-record it. It
            // never opted in after v260 defaulted no-backdrop callers to
            // the faux recipe - hence "transparent, no blur".
            modifier = if (glassOn) Modifier.liquidGlassCapsule(
                container,
                useGlobalCapture = true
            ) else Modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Category pill — auto-expands on entry showing icon + name.
                Surface(
                    shape = RoundedCornerShape(50),
                    color = categoryFill,
                    modifier = Modifier
                        .width(categoryWidth)
                        .height(RevealSentimentHeight)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CurioIcon(
                            name = cat.iconGlyph,
                            contentDescription = cat.displayName,
                            tint = categoryInk,
                            size = 24.dp
                        )
                        AnimatedVisibility(
                            visible = categoryExpanded,
                            enter = expandHorizontally(RevealExpandSpring, expandFrom = Alignment.Start) + fadeIn(RevealMotionSpring),
                            exit = shrinkHorizontally(RevealExpandSpring, shrinkTowards = Alignment.Start) + fadeOut(RevealMotionSpring)
                        ) {
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = ChangaOneFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 15.sp
                                ),
                                color = categoryInk,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 6.dp, end = 2.dp)
                            )
                        }
                    }
                }
                // v292 - Share pill: icon-only; tapping opens the topic
                // share-card sheet (frost card / aspect / fact source).
                Surface(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onShare()
                    },
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    modifier = Modifier
                        .width(RevealSentimentIconWidth)
                        .height(RevealSentimentHeight)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CurioIcon(
                            name = CurioIcons.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 26.dp
                        )
                    }
                }
                // Favorite pill — icon-only when not favorited, expands when favorited.
                Surface(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFavorite()
                    },
                    shape = RoundedCornerShape(50),
                    color = favFill,
                    modifier = Modifier
                        .width(favWidth)
                        .height(RevealSentimentHeight)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CurioIcon(
                            name = if (isFavorited) CurioIcons.Star else CurioIcons.StarOutline,
                            contentDescription = if (isFavorited) "Unfavorite" else "Favorite",
                            tint = favLabelInk,
                            size = 26.dp
                        )
                        AnimatedVisibility(
                            visible = favoriteShown,
                            enter = expandHorizontally(RevealExpandSpring, expandFrom = Alignment.Start) + fadeIn(RevealMotionSpring),
                            exit = shrinkHorizontally(RevealExpandSpring, shrinkTowards = Alignment.Start) + fadeOut(RevealMotionSpring)
                        ) {
                            Text(
                                text = "Favorite",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = ChangaOneFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 15.sp
                                ),
                                color = favLabelInk,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 6.dp, end = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** POST_NOTIFICATIONS is a no-op below API 33 — treated as granted. */
private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
