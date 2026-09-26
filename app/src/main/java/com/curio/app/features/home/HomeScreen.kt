package com.curio.app.features.home

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.curio.app.BuildConfig
import com.curio.app.R
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioPassport
import com.curio.app.data.laneKnowledge
import com.curio.app.data.CategoryFamily
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioQuests
import com.curio.app.data.PinnedTopic
import com.curio.app.data.TopicCatalog
import com.curio.app.data.TopicJsonLoader
import com.curio.app.data.SavedQuote
import com.curio.app.features.feedback.FeedbackFormCard
import com.curio.app.features.feedback.FeedbackFormState
import com.curio.app.features.personal.CreateEntrySheet
import com.curio.app.features.incursion.IncursionHomeButton
import com.curio.app.features.personal.PersonalCreateLauncher
import com.curio.app.features.personal.PersonalChipsRow
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioEntry
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.ExploreReminderScheduler
import com.curio.app.data.ExploreSession
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.markCompleted
import com.curio.app.data.StreakTracker
import com.curio.app.data.TourController
import com.curio.app.data.formatElapsed
import com.curio.app.ui.components.TornStatPaperShape
import com.curio.app.ui.components.curioDarkGlow
import com.curio.app.ui.components.curioGlassEdge
import com.curio.app.ui.components.heroStatPaneBase
import com.curio.app.ui.components.paperStatCardColor
import com.curio.app.ui.components.paperStatCardFill
import com.curio.app.data.formatSessionShort
import com.curio.app.data.openSearchUrl
import com.curio.app.features.settings.heroLaneCategory
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.materialHeroTearsOn
import com.curio.app.features.settings.settingsRoseAccent
import com.curio.app.infrastructure.ExploreSessionService
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToQuestRoute
import com.curio.app.navigation.navigateToTab
import com.curio.app.features.recent.RecentFeedItem
import com.curio.app.features.recent.buildRecentFeed
import com.curio.app.ui.adaptive.WideContentMaxWidth
import com.curio.app.ui.CurioLayout
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.theme.LocalCurioThemeTransition
import com.curio.app.ui.theme.switchThemeWithReveal
import com.curio.app.ui.components.CurioLaneDetailStrip
import com.curio.app.ui.components.LaneGridItem
import com.curio.app.ui.components.laneGridItems
import com.curio.app.ui.components.curioPressClickable
import com.curio.app.ui.components.CurioGlassToolbarMorph
import com.curio.app.ui.components.CurioDrawerState
import com.curio.app.ui.components.CurioForwardArrow
import com.curio.app.ui.components.CurioNavTint
import com.curio.app.ui.components.CurioPatientHold
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.glyphWatermarkDepthScale
import com.curio.app.ui.components.PaperTitleLines
import com.curio.app.ui.components.CurioMemberAvatar
import com.curio.app.ui.components.ProfileAvatarImage
import com.curio.app.ui.components.hasOwnPicture
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.curio.app.ui.components.SoftTornBottomShape
import com.curio.app.ui.components.SoftTornSheetShape
import com.curio.app.ui.components.SpinPickerRequest
import com.curio.app.ui.components.isInScreenGlassActive
import com.curio.app.ui.components.liquidGlassCapsule
import com.curio.app.ui.pet.CurioPetHome
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.activeNamedTheme
import com.curio.app.ui.theme.CurioDialogShape
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.curioDialogActionButtonColors
import com.curio.app.ui.theme.curioDialogContainerColor
import com.curio.app.ui.theme.curioDialogActionColor
import com.curio.app.ui.theme.curioFillInk
import com.curio.app.ui.theme.curioPillTintLift
import com.curio.app.ui.theme.curioTintOn
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.headerAccent
import com.curio.app.ui.theme.heroHeaderInk
import com.curio.app.ui.theme.isLiteMode
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.pastelAccent
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.toHsl
import com.curio.app.ui.theme.themedAccent
import com.curio.app.ui.theme.onAccent
import androidx.compose.runtime.withFrameNanos
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlinx.coroutines.launch

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
 *      paused sessions set aside for later (v474 — the section shows the
 *      NEWEST one and puts the rest behind a "N more" note that opens them in
 *      place, instead of a row per session).
 *   4. **Saved** — bookmarked quotes + pinned topics (hidden when empty),
 *      each row tappable through to its entry / topic.
 *   5. **Recents** — explored topics, unexplored topics (tagged
 *      "Unexplored"), and the latest saved entries as solid category-
 *      tinted cards (View all → Cabinet), or a beautiful empty-state card
 *      prompting the first spin.
 *   6. **Reminder CTA** (only when the reminder is OFF *and* Recents is
 *      still empty) — a subtle ghost-style card suggesting the user try a
 *      daily shuffle reminder, navigating to Settings. v474 — the second
 *      half of that condition is new: standing under a full Recents list it
 *      was a sixth block on a five-block page, so it now belongs to the
 *      first-run page it was written for. The setting itself is untouched
 *      (Settings → Preferences → "Daily shuffle reminder").
 *
 *  v147 — the drawer itself now lives at the NavHost root (drawn ABOVE the
 *  floating pill bar, which stays composed underneath): Home's hamburger
 *  raises the request via [CurioDrawerState.requestOpen]. The drawer still
 *  wears the torn-rose hero family (v7.89) for secondary navigation
 *  (Quests, History, Manage Categories, Browse Topics, Support).
 */
/** The quest hero's solid body height — the torn banner. Tall enough for
 *  the greeting + the Streak · Cabinet · Recent bar (pinned just above the
 *  tear) and generous at large font scales.
 *  v411 — +80dp, because the DAILY QUEST now lives inside the banner too (the
 *  member: "extend the home screen tear more and put the todays quest shuffle
 *  the deck inside it"): greeting, stat bar and the Shuffle CTA are one hero
 *  now, instead of a banner with a CTA parked below the seam. */
private val HomeQuestHeroHeightPortrait = 380.dp
/** Landscape hero — shorter to leave room for content below (+66, the same
 *  growth read against a shorter window). */
private val HomeQuestHeroHeightLandscape = 296.dp
/** Extra layout space reserved for the white sheet below the torn banner. */
private val HomeQuestSheetExtent = 24.dp
/** Scroll distance (dp) before the menu + profile pills fully pin as
 *  frosted floating pills. */
private val StickyBarThreshold = 90.dp
// v3xx22 — the morphing glass header's collapsed height (below the status
// bar): the slim identity bar holding the menu pill + avatar + greeting.
private val HomeCompactHeaderHeight = 54.dp
/** v3xx22 — the FULL glass toolbar's resting footprint (status bar + title
 *  row + stat row) — the scroll content reserves this so the pinned morph
 *  bar never covers the first real card at rest (a hair of slack is fine;
 *  the bar is content-height). */
private val HomeGlassToolbarFullHeight = 200.dp
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
    // Satisfying haptics: confirm on the big spin CTA, light ticks on picks.
    val haptics = LocalHapticFeedback.current
    // v30 — Appearance "Hero follows Spin lane": the quest hero AND the Home
    // background take the category last picked on Spin (the Cabinet's
    // language) when the toggle is on; otherwise Home stays on the soft
    // rose-tinted background with the rose/azure hero. (The v27u Home tint
    // experiments were removed — this is their always-clean successor.)
    // v414 — Home's page resolves through the shared `heroPageBackground`: the
    // Adaptive Hero lane's wash first, else the CATEGORY TINT lane's wash (when
    // that Appearance switch is on and a lane was last picked on Spin), else the
    // soft rose-tinted default. Before v414 Home ignored the Category tint row
    // entirely — it fell straight to the rose lerp unless Adaptive Hero was on,
    // which is why the switch looked dead here.
    val homeBg = heroPageBackground(
        androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.background,
            settingsRoseAccent(),
            0.10f
        )
    )
    // The hero + sticky top-bar pills share the SAME resolved fill (the
    // lane-aware homeRoseAccent below) so the menu/profile pills match the
    // quest hero in every mode.
    val heroFill = homeRoseAccent()
    // v28 — dark mode: the hero's title + sticky pills stay white/creamish
    // (never a tinted light twin over the deep banner); light mode keeps the
    // pastel-aware on-accent ink.
    val questInk = homeReadableInk(heroFill)
    // Publish the page's real background (lane wash OR the rose-tinted
    // default) so the Scaffold-level bottom nav blends with Home even
    // without a lane — a plain-surface slot behind the floating pill read
    // as a visible strip (v125).
    LaunchedEffect(homeBg, heroFill) {
        CurioNavTint.publishHomeWash(homeBg)
        // v149 — publish Home's rose accent so the floating nav bar's ACTIVE
        // pill wears it (falls back to secondary on pages without one).
        CurioNavTint.publishHomeAccent(heroFill)
    }
    val displayName = AppPreferences.displayNameState
    // Saved-shelf unsave confirmation — set when the user taps the remove
    // bookmark on a saved quote row; the dialog confirms before removal.
    var pendingUnsave by remember { mutableStateOf<SavedQuote?>(null) }
    // Unpin-topic confirmation �� set when the user taps unpin on a pinned
    // topic row; the dialog confirms before the pin is dropped.
    var pendingUnpin by remember { mutableStateOf<PinnedTopic?>(null) }
    // v387 — the writing sheet behind the floating "+" (a journal page or a
    // book). The button itself hides while the page is scrolled down, so a
    // long read is never covered by it.
    var writeSheetOpen by remember { mutableStateOf(false) }
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
            // v9.x — the bed hosts only the quest-complete nudge; play stays
            // available through the pet's own interactions.
            // One-shot after a quest claim: consume the pending marker in a
            // LaunchedEffect (never during composition — backwards write).
            var nudgeBubble by remember { mutableStateOf(false) }
            LaunchedEffect(CurioPet.pendingQuestNudge) {
                if (CurioPet.consumeQuestNudge()) {
                    nudgeBubble = true
                    delay(3200)
                    nudgeBubble = false
                }
            }
            Box(contentAlignment = Alignment.Center) {
                PetLandmark(
                    id = "bed",
                    kind = PetLandmarks.Kind.PLAY,
                    screen = "home"
                ) { m ->
                    // v407 — the pet's home is HELD to act (turn the pet
                    // off), so it wears the app's patient hold: a tap still
                    // wakes / brings the pet out, only a real hold opens the
                    // offer, and scrolling can never arm it.
                    CurioPatientHold {
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
                // One-shot quest-complete celebration bubble.
                if (nudgeBubble && CurioPet.awake) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-6).dp)
                    ) {
                        Text(
                            text = "Quest done! ✨",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    } else null
    // ── v457 — HOME'S FEED STARTS FROM THE LAST LOOK (see [HomeFeedSnapshot]) ──
    //
    // The member: *"many ui elements have loading unloading behaviors
    // unnecessarily"*. This read started at `emptyList()` on EVERY composition,
    // so coming back to Home dropped the Recents rows and the drawer's own
    // knowledge map to nothing and filled them in a frame or two later — the
    // unload-and-load they were describing, for an archive that had not changed.
    // The snapshot is the first frame now, and the read only ever corrects it. A
    // FAILED read keeps what is on screen rather than blanking the feed, which is
    // the same rule the store's own best-effort loads follow.
    val recentEntries by produceState<List<CurioEntry>>(initialValue = HomeFeedSnapshot.entries.take(5)) {
        try {
            val loaded = CurioRepositoryHolder.repo.getAll()
            HomeFeedSnapshot.entries = loaded
            value = loaded.take(5)
        } catch (_: Exception) {
            // Nothing to do: the snapshot stands.
        }
    }
    val exploredTopics = ExploreSessionStore.recentlyExploredState
    val unexploredTopics = ExploreSessionStore.recentlyUnexploredState
    // v472 — the finished topics ride with the explored ones (see
    // `ExploreSessionStore.doneRecents`): they carry no timestamp, so the feed's
    // dedupe prefers any row with a real time and they sort behind every genuine
    // recent discovery rather than in front of one.
    val finishedTopics = remember(ExploreSessionStore.doneTopicsState) {
        ExploreSessionStore.doneRecents()
    }
    val recentFeed = remember(recentEntries, exploredTopics, unexploredTopics, finishedTopics) {
        buildRecentFeed(recentEntries, exploredTopics, unexploredTopics, finishedTopics)
    }
    var totalSaved by remember { mutableIntStateOf(0) }
    // v27h — the Topics stat always shows the TRUE catalog total: the
    // splash warm-cache seeds the first frame, then a lightweight IO count
    // of the JSON assets refreshes it — so the number never reads 0 just
    // because the database/catalog hasn't finished loading, and it tracks
    // content drops. (Hoisted — the glass toolbar stat row reads it too.)
    val topicsTotal by produceState(initialValue = TopicCatalog.totalTopicCount()) {
        value = TopicJsonLoader.countCanonicalTopics()
    }
    LaunchedEffect(Unit) {
        try {
            totalSaved = CurioRepositoryHolder.repo.count()
        } catch (_: Exception) {}
    }

    val navInsets = WindowInsets.navigationBars.asPaddingValues()

    // v147 — the Home drawer now lives at the NavHost root (CurioNavHost):
    // it renders ABOVE the floating pill bar while the bar stays composed
    // underneath — no more hide-and-reappear. Home's hamburger requests it
    // via [CurioDrawerState.requestOpen]; the NavHost owns the DrawerState.
    // This plain Box is the page's own wrapper (the drawer's old content
    // slot), so the page paints full-bleed exactly as before.
    Box(modifier = Modifier.fillMaxSize()) {
        // v6.7 — Home sits on the plain theme background (the category tint
        // wash was removed from Home); v27u — the "Home tint" experiment can
        // restore a category-tinted background.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(homeBg)
        ) {
            // Hoisted scroll state — read by the sticky-bar block below
            // (a sibling of the v241 capture wrapper).
            val homeScroll = rememberScrollState()
            // v3xx43 — the collapse clock is hoisted ABOVE the scroll content:
            // it drives the pinned morph bar AND the space the page reserves
            // for it, so the header genuinely collapses (the old static 200dp
            // reservation kept the page looking permanently expanded after
            // scrolling — the reported "doesn't collapse" bug).
            // The collapsed bar still owns the status-bar strip (the glass
            // fills it now), so the reservation floors at compact + inset.
            val statusTopDp = with(LocalDensity.current) {
                WindowInsets.statusBars.getTop(this).toDp()
            }
            // v3xx49 — the collapse clock runs the DISTANCE THE HEADER CAN
            // ACTUALLY GIVE BACK (full reservation − the compact floor), not a
            // fixed 90dp. A 90dp clock against a ~120dp space reclaim meant
            // the page slid up ~1.3× faster than the finger for 90dp and then
            // snapped back to 1:1 — the reported "jump/flicker at the collapse
            // point". Matching the two makes the header track the scroll
            // exactly: whatever the finger takes, the header gives back.
            val homeCollapsePx = with(LocalDensity.current) {
                if (AppPreferences.headerStyleState == AppPreferences.HeaderStyle.GLASS) {
                    (HomeGlassToolbarFullHeight - (HomeCompactHeaderHeight + statusTopDp))
                        .coerceAtLeast(1.dp)
                        .toPx()
                } else {
                    // Torn-paper style: the clock still only drives the floating
                    // pills' frost morph, which is tuned to 90dp.
                    StickyBarThreshold.toPx()
                }
            }
            val homeStickyProgress by remember {
                derivedStateOf { (homeScroll.value / homeCollapsePx).coerceIn(0f, 1f) }
            }
            // ── v452 — AND THE RESERVATION IS THE BAR'S OWN HEIGHT ──────
            //
            // The glass header is ONE state now AND it is the content-height bar
            // (`CurioGlassToolbarMorph` pins `eased = 0f` — see its note), so the
            // page reserves exactly what that bar reports: the full resting
            // footprint, at every scroll position. It used to lerp down to the
            // compact floor on `progress`, which no longer drives the header at
            // all — a reservation that shrinks under a bar that does not would
            // slide the first card up behind the glass.
            val glassHeaderReserve = HomeGlassToolbarFullHeight
            // v241 — LOCAL GLASS CAPTURE: everything BEHIND the floating
            // top-bar pills records into its own layer; pills are a SIBLING
            // overlay outside this wrapper (the bottom-nav architecture —
            // no self-capture cycle by construction).
            val homeGlassBackdrop = rememberLayerBackdrop()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(homeBg)
                    .layerBackdrop(homeGlassBackdrop)
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
            // v243 — the DUPLICATE declaration is gone: this used to shadow
            // the hoisted `homeScroll` above, so the sticky bar read a state
            // that never scrolled and the glass morph never started. The
            // scroll Column now uses the hoisted state the bar also reads.
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
            // v411 — THE SHUFFLE ACTION, hoisted out of the old below-hero
            // call site so the quest can live INSIDE the banner. One lambda,
            // one behaviour: the tour's quest step first, else a genuinely
            // fresh random deck (single lane or a mix) that bypasses the
            // generic Spin tab restore.
            val onQuestShuffle: () -> Unit = {
                if (TourController.consumeTap("quest")) {
                    TourController.routeForCurrentStep()?.let { nextRoute ->
                        // v123 — the tour's tab steps navigate via
                        // navigateToQuestRoute so HOME stays in the
                        // NavController's saved-state map; a plain push made
                        // the later Home-tab tap restore the popped Spin stack
                        // ("Home dead" after skipping the tour on Spin).
                        navController.navigateToQuestRoute(nextRoute)
                    }
                } else {
                    // v7.94 — shuffle only VISIBLE lanes: hidden categories
                    // (Manage Categories) never get dealt.
                    val all = CurioCategories.visible
                    val pickMix = Random.nextBoolean()
                    val chosen =
                        if (pickMix) all.shuffled().take(2 + Random.nextInt(2))
                        else listOf(all.random())
                    AppPreferences.setLastSpinCategories(context, chosen.map { it.id })
                    // Keep the random single/mix selection intact, but bypass
                    // the generic tab restore here. Restoring a previous Spin
                    // composition can hide this newly chosen deck and make
                    // every tap look like the same category.
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
            }
            val heroTornShape = remember(HOME_TEAR_SEED) { SoftTornBottomShape(HOME_TEAR_SEED, bold = true) }
            val sheetShape = remember(HOME_TEAR_SEED) {
                SoftTornSheetShape(HOME_TEAR_SEED, lip = 10.dp, baseline = 14.dp, bold = true)
            }
            // Adaptive hero height — shorter in landscape to leave room for content
            // v479 — the short height follows a SHORT WINDOW, not merely a wide
            // one: a split-screen window under 600dp wide but under 520dp tall was
            // taking the 380dp portrait banner, which is taller than it. Both tests
            // are true for a landscape phone; only the height is true for a short
            // half-screen. A landscape TABLET is not short, so it keeps its room.
            val homeHeroHeight = if (windowWidthSizeClass().isWide || CurioLayout.isShortWindow())
                HomeQuestHeroHeightLandscape else HomeQuestHeroHeightPortrait
            // The quest is always the wildcard Surprise now (the category
            // chip row is gone). The banner wears the muted rose-wood hero
            // accent — in pastel mode (the shipped default) it resolves to
            // the airy rose-wood pastel twin, otherwise the calm base.
            // v27u/v27v — hero tint is resolved at the TOP of the screen
            // (shared with the sticky pills); questInk = the readable ink on
            // the active fill, carried through greeting, stat icons + watermark.

            // v3xx — GLASS TOOLBAR style: the app-wide "Glass toolbar
            // header" option swaps Home's torn quest banner for the PINNED
            // MORPHING glass bar (rendered as a sibling overlay in the
            // sticky-bar slot below — greeting + name + the Streak ·
            // Cabinet · Topics stat row in the full bar, collapsing to a
            // slim "Good morning Jugnu" identity bar on scroll). This scroll
            // slot only RESERVES the full bar's resting footprint so the
            // pinned bar never covers the first real card (v3xx22 — the
            // user asked for the morph collapse on Home again).
            if (AppPreferences.headerStyleState == AppPreferences.HeaderStyle.GLASS) {
                Spacer(Modifier.height(glassHeaderReserve))
            } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(homeHeroHeight + HomeQuestSheetExtent)
            ) {
                // ── White under-sheet — same as the detail hero's: the
                // sheet's torn top hides behind the opaque banner while its
                // uneven lip reads white below the tear, and the page wash
                // starts right after it.
                // v108 — OFF by default (Settings → Experiments → Paper &
                // headers): the hero tears straight into the page; the
                // toggle restores this extra paper layer.
                if (AppPreferences.heroTearSheetState) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .offset(y = homeHeroHeight - 18.dp)
                        .clip(sheetShape)
                        // v81 — dark: a subtle lighter lip under the tear so
                        // the paper seam still reads on the dark banner.
                        .background(
                            if (isCurioDarkTheme()) lerp(heroFill, Color.White, 0.10f)
                            else Color(0xFFFDFCF9)
                        )
                )
                }
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
                        .height(homeHeroHeight)
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
                        .height(homeHeroHeight)
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
                            // name beneath it. Proper hierarchy (v411, the
                            // member: "make the home screen welcome back
                            // curiours explorer ... font hirarcy more better
                            // bigger cleaner"): the greeting and the NAME are
                            // two halves of ONE sentence, so the greeting is a
                            // light 26sp line (not a heavy kicker competing
                            // with the name) and the name under it is the
                            // heavy 40sp star. The commas and the weight gap
                            // are what make the pair read as a sentence.
                            // v8.16 — the greeting is a CURIOUS pet landmark:
                            // the pet sometimes tiptoes over and reads it
                            // (the text itself just pulses — no layout move).
                            val greeting = homeGreeting()
                            PetLandmark(
                                id = "greeting",
                                kind = PetLandmarks.Kind.CURIOUS,
                                screen = "home"
                            ) { m ->
                                Text(
                                    text = "$greeting,",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 26.sp
                                    ),
                                    color = curioTintOn(heroFill, questInk, 0.86f),
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
                                    // v411 — 36sp → 40sp: the name is the
                                    // hero's star and the taller banner has the
                                    // room for it.
                                    fontSize = 40.sp,
                                    // Fixed ~50sp leading box held against font scaling (min 42sp).
                                    // Plain Float math: TextUnit has no coerceAtLeast (it only
                                    // exposes an operator compareTo, not the Comparable bound).
                                    lineHeight = (50f / nameFontScale.coerceAtLeast(1f)).coerceAtLeast(42f).sp
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
                                PaperTitleLines(
                                    ink = questInk,
                                    title = displayName,
                                    fontSize = 36.sp
                                )
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
                            // v27u — shared paper color (same cream/rose-brown
                            // blend Profile's stat pane uses).
                            // v476 — and the non-paper pane's base is the hero tint
                            // LIFTED, so it reads as a raised card rather than as part
                            // of the banner (the glass header's own shade — the member
                            // asked for the torn header to match it).
                            val statPaneBase = heroStatPaneBase(heroFill)
                            val paperStatBg = paperStatCardColor(heroFill)
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
                            val ringsOn = holesOn && AppPreferences.paperHoleRingsState
                            // v27v — which 3D ring look the holes wear.
                            val ringStyle = AppPreferences.paperHoleRingStyleState
                            // v200 — the Surface wrapper is GONE: M3 Surface
                            // (1.2+) clips its children to the shape, which CUT
                            // the coil's left peek at the card edge. A plain
                            // Box + shadow(clip = false) keeps the elevation
                            // without the clip — the paper fill self-clips to
                            // the shape outline, so the protruding wire can
                            // render outside the card.
                            // v27u — the paper surface (fill + 3-hole column
                            // + pressed rims or tilted book rings) lives in the
                            // shared paperStatCardFill component, so Profile's
                            // stat pane wears the same card.
                            // v74 — the pane always carries the elevation + dark
                            // glow, exactly like Profile's stat pane.
                            Box(
                                modifier = Modifier
                                    .curioDarkGlow(3.dp, statShape)
                                    .shadow(3.dp, statShape, clip = false)
                                    .then(
                                        when {
                                        paperStatsOn -> Modifier.paperStatCardFill(
                                            shape = statShape,
                                            fill = paperStatBg,
                                            holesOn = holesOn,
                                            ringsOn = ringsOn,
                                            ringStyle = ringStyle,
                                            ink = questInk,
                                            // v81 — dark: light metal ring tones.
                                            dark = isCurioDarkTheme()
                                        )
                                        else -> Modifier.background(
                                            // v74 — OPAQUE theme-aware pane, the
                                            // same construction as Profile's stat
                                            // pane: the old 12–55% alpha glass
                                            // read transparent and let the
                                            // elevation shadow bleed through.
                                            // The opaque blends resolve to the
                                            // same perceived tints over the
                                            // banner while keeping the shadow
                                            // clean (theme-aware like Profile).
                                            Brush.verticalGradient(
                                                listOf(
                                                    lerp(statPaneBase, Color.White, 0.06f),
                                                    lerp(statPaneBase, Color.White, 0.26f)
                                                )
                                            ),
                                            RoundedCornerShape(20.dp)
                                        )
                                    }
                                )
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
                                            value = "$streakDays",
                                            label = "Streak",
                                            tint = questInk,
                                            ink = questInk,
                                            modifier = Modifier.weight(1f),
                                            onClick = { navController.navigate(CurioRoutes.QUESTS) { launchSingleTop = true } }
                                        )
                                        VerticalDivider(
                                            modifier = Modifier.height(34.dp),
                                            color = questInk.copy(alpha = 0.22f)
                                        )
                                        HeroStatSegment(
                                            glyph = CurioIcons.Inventory2,
                                            value = "$totalSaved",
                                            label = "Cabinet",
                                            tint = questInk,
                                            ink = questInk,
                                            modifier = Modifier.weight(1f),
                                            onClick = { navController.navigateToTab(CurioRoutes.CABINET) }
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
                                            modifier = Modifier.weight(1f),
                                            onClick = { navController.navigate(CurioRoutes.DATABASE) { launchSingleTop = true } }
                                        )
                                    }
                                }
                                // ── v411 — THE DAILY QUEST, INSIDE THE TEAR ──
                                // The member: "extend the home screen tear more
                                // and put the todays quest shuffle the deck
                                // inside it". The banner is 80dp taller and the
                                // quest now sits ON it, under the stat bar — so
                                // Home's whole top is ONE hero instead of a
                                // banner with a CTA parked below the seam.
                                Spacer(Modifier.height(16.dp))
                                // v8.25 — the quest block is the tour's HOME
                                // landmark: the First Journey's welcome step
                                // highlights the real TODAY'S QUEST card
                                // instead of a guessed bottom zone.
                                PetLandmark(
                                    id = "quest",
                                    kind = PetLandmarks.Kind.FUN,
                                    screen = "home"
                                ) { m ->
                                    QuestShuffleCard(
                                        // A paper-white disc ON the rose banner:
                                        // the pastel accent would vanish into
                                        // its own hero, so the plate is the
                                        // banner lifted toward white. v412 — the
                                        // glyph must read ON THAT DISC, not on
                                        // the banner: dark mode's questInk is the
                                        // near-white onBackground (right for the
                                        // banner's copy, invisible on a near-white
                                        // disc), so the disc's ink walks the deep
                                        // same-hue ink [curioFillInk] resolves on
                                        // the plate.
                                        plate = lerp(heroFill, Color.White, 0.88f),
                                        ink = curioFillInk(lerp(heroFill, Color.White, 0.88f)),
                                        copyInk = questInk,
                                        pet = homePetSprite,
                                        onShuffle = onQuestShuffle,
                                        modifier = m
                                    )
                                }
                        }
                    }
                }
                // The menu + profile pills no longer live here — they moved
                // to a scroll-reactive STICKY bar outside the hero (they pop
                // out of the coral into frosted floating pills on scroll).
            }
            } // v3xx — end of the torn-hero branch (glass toolbar else)

            // v411 — THE DAILY QUEST IS NOT HERE ANY MORE: it moved up INTO
            // the torn banner (see the hero's own QuestShuffleCard call), so
            // the space it parked in — the 26dp breathing room and its
            // centered column — went with it. The banner's under-sheet extent
            // is what separates the hero from the content now.
            // v49 — one consistent 12dp section rhythm below the shuffle
            // deck: the old 20dp ends stacked with the 20dp spacer before
            // Saved (40dp of dead space when no session/queue is live).
            Spacer(Modifier.height(12.dp))

            // v323 — the Home "Today's quests" strip was removed per user
            // direction (quests live on the Quests screen only).
            Spacer(Modifier.height(12.dp))

            // ── The feedback form, while one is live (v403) ────────────────
            // The member asked for the form to greet everyone who opens the
            // app, so it leads Home under the deck: the page's own standout
            // tile, with the two ways out beside it and Support keeping the
            // door for later. Nothing else on Home changes shape when a form
            // is live or when none is.
            LaunchedEffect(Unit) { FeedbackFormState.refresh(context) }
            val liveFeedbackForm = FeedbackFormState.liveForm
            if (liveFeedbackForm != null && FeedbackFormState.visible) {
                FeedbackFormCard(
                    form = liveFeedbackForm,
                    onOpen = { FeedbackFormState.openSheet() },
                    onSkip = { FeedbackFormState.skip(context, liveFeedbackForm, never = false) },
                    onNever = { FeedbackFormState.skip(context, liveFeedbackForm, never = true) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .widthIn(
                            max = if (windowWidthSizeClass().isWide) WideContentMaxWidth
                            else Dp.Infinity
                        )
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(12.dp))
            }

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
                        // v470 — writing it down means the topic is finished:
                        // the same completed mark the shade's "Completed" action
                        // and the back-to-app dialog write.
                        activeSession.markCompleted(context)
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
                        openSearchUrl(context, activeSession.searchUrl)
                    },
                    onStop = {
                        // Top-corner stop — quiet teardown, same as the
                        // notification's Cancel action (no write-it-down
                        // page, no done prompt on the next return).
                        // v226 — stash first so the session is recoverable
                        // from Home's cancelled-explore card.
                        ExploreSessionStore.stashCancelledSession(context, activeSession)
                        ExploreSessionStore.clearSession(context)
                        ExploreReminderScheduler.cancel(context)
                        ExploreSessionService.stop(context)
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── 3. Queued explores — sessions set aside for later ──────
            // When a new explore replaced the running one, the old session is
            // paused (time banked) and queued here. Tap a row to swap it back
            // into the active slot; the ✕ discards it.
            // v474 — one row by default (the newest) with the rest behind the
            // "N more" note — see the block below for why, and for the rule that
            // keeps an odd fifth session reachable.
            val queuedSessions = ExploreSessionStore.queuedSessionsState
            // ── v474 — ONE ROW, AND THE REST BEHIND IT ────────────────────
            //
            // The member's own pick (from "a row each" and this): a member who
            // has set three explores aside was reading three rows on Home for
            // work they had already decided to put off. The **newest** is the one
            // shown — the queue is newest-first (see `ExploreSessionStore`'s own
            // `listOf(paused) + readQueued(...)`) — and the ones behind it are
            // never lost: the note under the row opens the rest IN PLACE, so
            // nothing needed a new page, and no session became unreachable.
            var queuedExpanded by remember { mutableStateOf(false) }
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
                        // v474 — the count belongs in the heading once the list
                        // is collapsed: it is the one thing the hidden rows were
                        // saying, in three words instead of three cards.
                        if (queuedSessions.size > 1) {
                            "Queued explores \u00b7 ${queuedSessions.size}"
                        } else {
                            "Queued explores"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val shown =
                            if (queuedExpanded) queuedSessions else queuedSessions.take(1)
                        shown.forEachIndexed { index, queued ->
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
                    if (queuedSessions.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (queuedExpanded) "Show less" else "${queuedSessions.size - 1} more",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = curioDialogActionColor(),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { queuedExpanded = !queuedExpanded }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── 3b. Cancelled explore — recoverable until discarded ────
            // v226 — cancelling no longer eats the session: the last
        // cancelled one shows here as a resumable row (banked time
            // continues where the cancel stopped it). Gated on NO active
            // session — reviving replaces the slot.
            val cancelledSession = ExploreSessionStore.cancelledSessionState
            if (cancelledSession != null && activeSession == null) {
                CancelledExploreRow(
                    session = cancelledSession,
                    onResume = {
                        val revived = ExploreSessionStore.resumeCancelledSession(context)
                        if (revived != null) {
                            ExploreReminderScheduler.schedule(
                                context, revived.startMillis, revived.durationMinutes
                            )
                            if (AppPreferences.exploreServiceShouldRun(context)) {
                                ExploreSessionService.start(context, revived)
                            }
                        }
                    },
                    onDiscard = { ExploreSessionStore.clearCancelledSession(context) }
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── 4. v387 — YOUR WRITING (journals + books) ──────────────
            // This slot held the Saved shelf (bookmarked quotes + pinned
            // topics); those live in Topic History, and Home's shelf is now
            // the member's OWN writing — the journals and the books, as
            // small fixed-shape chips that open straight into their pages.
            PersonalChipsRow(
                navController = navController,
                // The doors' plate has to be the paint this page is actually
                // wearing (a lane wash, or Home's own rose tint), or it shows as
                // a pale rectangle across the row (see PinnedDoorRow).
                backdrop = homeBg,
                modifier = Modifier
                    // Wide windows: the same comfortable centered column the
                    // sections above and below ride in (phone untouched).
                    .widthIn(max = if (windowWidthSizeClass().isWide) WideContentMaxWidth else Dp.Infinity)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(12.dp))

            // ── 5. Recents — explored + unexplored topics and recent entries ──
            //
            // v387 — the saved-capture rows left Home's recents: the member's own
            // writing is the Home shelf now, so this list is explored /
            // unexplored topics only (the saved archive keeps its Cabinet, its
            // Recents page and its detail view).
            //
            // v474 — HOISTED out of the section's own Column: the reminder nudge
            // below (section 6) hides itself while there is nothing here yet, so
            // the emptiness has to be readable one block further down than it
            // was declared. Same value, same `remember` key — nothing recomputes.
            val recentPreview = remember(recentFeed) {
                recentFeed.filterNot { it is RecentFeedItem.SavedEntry }.take(5)
            }
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
                        "Recents",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (recentPreview.isNotEmpty()) {
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
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                CurioForwardArrow(
                                    "Open Recents",
                                    tint = MaterialTheme.colorScheme.onBackground,
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
                        onPickCategory = {
                            // v142 ��� wire the first-run "Pick a lane" to the
                            // SPIN screen's own category picker sheet (the same
                            // lane chips + Mix presets the deck uses) instead
                            // of the separate full-screen picker page.
                            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            SpinPickerRequest.pending = true
                            navController.navigateToTab(CurioRoutes.SPIN)
                        },
                        onShuffleSurprise = {
                            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            navController.navigateToTab(CurioRoutes.SPIN)
                        }
                    )
                } else {
                    // v455 — HOME'S RECENTS ARE TAP-ONLY.
                    //
                    // The member: *"the tap and hold is buggy in home screen
                    // recent topics, even after when im not holding and im
                    // releasing it continues the holding and its buggy …
                    // remove the tap and hold action from home screen
                    // recents"*. So the gesture is GONE from these rows — not
                    // re-tuned. A hold that outlives the finger is the radial
                    // picker arming on its own (it can be entered from the
                    // gesture's own cancellation window), and no timeout
                    // tuning removes a race that lives below it; a row that
                    // only opens its topic cannot get stuck holding.
                    //
                    // What moved: the three hold actions (Write about it,
                    // Remove from Recents, Open saved entry) are the RECENTS
                    // PAGE's own rows, which is where a member goes to manage
                    // the feed. Home's rows tap into the topic, and that is
                    // all they do.
                    //
                    // NOTE the pet's bed above keeps `CurioPatientHold` — that
                    // hold is a different gesture on a different view, and it
                    // is not part of this removal.
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
                                        // v474 — the state, and nothing else. "· tap to
                                        // open" was an instruction the chevron already
                                        // gives.
                                        subtitle = "Explored",
                                        onClick = {
                                            navController.navigate(
                                                CurioRoutes.revealFor(explored.categoryId.routeSlug, explored.topicName)
                                            ) { launchSingleTop = true }
                                        },
                                    )
                                }
                                is RecentFeedItem.Unexplored -> {
                                    val unexplored = item.topic
                                    ExploreTopicRow(
                                        category = CurioCategories.byId(unexplored.categoryId),
                                        topicName = unexplored.topicName,
                                        // v476 — the row says it ONCE. It wore the
                                        // "Unexplored" chip AND a "Not explored"
                                        // subtitle, which is the same fact twice
                                        // (the member: *"in home there are 2 info
                                        // duplicate"*); asked which to keep, they
                                        // chose the CHIP, so the subtitle is blank
                                        // and ExploreTopicRow skips it.
                                        tag = "Unexplored",
                                        subtitle = "",
                                        onClick = {
                                            navController.navigate(
                                                CurioRoutes.revealFor(unexplored.categoryId.routeSlug, unexplored.topicName)
                                            ) { launchSingleTop = true }
                                        },
                                    )
                                }
                                is RecentFeedItem.SavedEntry -> {
                                    RecentEntryRow(
                                        entry = item.entry,
                                        onClick = {
                                            navController.navigate(
                                                CurioRoutes.revealFor(item.entry.topic.categoryId.routeSlug, item.entry.topic.name)
                                            ) { launchSingleTop = true }
                                        },
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
            //
            // v474 — AND ONLY WHILE HOME HAS NOTHING TO SHOW YET. Standing alone
            // under a full Recents list it was a sixth block on a page that
            // already had five, asking for a setting nobody came to Home for;
            // folded into the first-run page it is the last thing a new member
            // reads, which is the member who wants a daily nudge. The setting
            // itself never moves — Settings → Preferences → "Daily shuffle
            // reminder" — so nothing is lost for anyone who explored already.
            if (!reminderEnabled && recentPreview.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                ReminderNudgeCard(
                    surface = MaterialTheme.colorScheme.surfaceContainerLow,
                    onTap = { navController.navigate(CurioRoutes.SETTINGS) { launchSingleTop = true } }
                )
            }

            // v129 — the pill bar floats over the page now (Scaffold slot
            // removed), so on phones the content clears it with extra room;
            // wide windows use the rail instead and keep the old spacing.
            // v131 — clearance grew with the bigger pill (92 → 100dp).
            Spacer(Modifier.height(if (windowWidthSizeClass().isWide) 32.dp else 100.dp))
            Spacer(Modifier.height(navInsets.calculateBottomPadding()))
            }
            } // v241 — end of the local glass capture subtree

            // ── v387 — THE WRITING "+" — a fixed accent disc that slips away
            // while the page is being scrolled DOWN and comes back the moment
            // the finger goes up (or the page reaches the top), so a long read
            // is never covered by a button nobody asked for mid-scroll. It is
            // the door to a journal page or a book.
            var createVisible by remember { mutableStateOf(true) }
            LaunchedEffect(homeScroll) {
                var previous = homeScroll.value
                snapshotFlow { homeScroll.value }.collect { now ->
                    val delta = now - previous
                    if (delta > 6) createVisible = false
                    else if (delta < -6 || now <= 0) createVisible = true
                    previous = now
                }
            }
            PersonalCreateLauncher(
                visible = createVisible,
                onClick = { writeSheetOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp)
                    .padding(bottom = 92.dp + navInsets.calculateBottomPadding())
            )

            // ── v389 — INCURSION's door, and ONLY for a member who has typed
            // the phrase into a search field somewhere in the app. It draws
            // nothing, and reserves nothing, until then (the component itself
            // checks the lock), so the app has no visible trace of the page for
            // anyone else. It rides one slot above the writing "+" — 92dp of
            // nav clearance, the disc's own 56dp, and a 10dp gap — and slips
            // away on a downward scroll with it, because Home's floating
            // furniture should behave like one family.
            IncursionHomeButton(
                visible = createVisible,
                onClick = {
                    navController.navigate(CurioRoutes.INCURSION) { launchSingleTop = true }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp)
                    .padding(bottom = 158.dp + navInsets.calculateBottomPadding())
            )

            if (writeSheetOpen) {
                CreateEntrySheet(
                    onDismiss = { writeSheetOpen = false },
                    onJournal = {
                        writeSheetOpen = false
                        navController.navigate(
                            CurioRoutes.journalEditor(CurioRoutes.PERSONAL_NEW)
                        ) { launchSingleTop = true }
                    },
                    onBook = {
                        writeSheetOpen = false
                        navController.navigate(CurioRoutes.BOOKS) { launchSingleTop = true }
                    },
                    // v389 — the two doors open their OWN pages now: the note
                    // page offers its topic picker (catalog search or a typed
                    // name) in front of the writing, and the list page opens as
                    // a checklist. Neither is a journal day any more.
                    onTopicNote = {
                        writeSheetOpen = false
                        navController.navigate(
                            CurioRoutes.topicNote(CurioRoutes.PERSONAL_NEW)
                        ) { launchSingleTop = true }
                    },
                    onTodoList = {
                        writeSheetOpen = false
                        navController.navigate(
                            CurioRoutes.todo(CurioRoutes.PERSONAL_NEW)
                        ) { launchSingleTop = true }
                    },
                    // v449 — and the dictionary's own page, so a word that turns up
                    // while the member is looking at Home is one tap from its
                    // meaning (the other door is the reader's ⋯ menu).
                    onDictionary = {
                        writeSheetOpen = false
                        navController.navigate(CurioRoutes.READER_DICTIONARY) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ── Sticky top bar — menu + profile pills ─────────────────
            // Pinned OUTSIDE the scroll content so they stay on screen.
            // Resting on the hero they use a solid accent fill; as the hero
            // scrolls away they continuously fade into solid floating
            // frosted pills. The scale is tied directly to the same eased
            // progress, so there is no post-pop bounce or rotation wobble.
            // v3xx43 — the raw scroll progress is hoisted above the scroll
            // content now (it also drives the glass header's reservation).
            // One scroll-linked clock drives color, scale, lift and shadow.
            // FastOutSlowIn gives the fade a gentle start and finish while
            // keeping it perfectly scrubable with the user's finger.
            // v3xx22 — GLASS style: the pinned MORPHING toolbar replaces the
            // floating pills (it carries its own menu + avatar and collapses
            // from the full greeting+stats bar to a slim "Good morning
            // Jugnu" identity bar on scroll — the user asked for the morph
            // collapse on Home again). The torn style keeps the classic
            // always-floating menu/avatar pills below.
            val homeGlassOn = AppPreferences.headerStyleState == AppPreferences.HeaderStyle.GLASS
            val frostShift = FastOutSlowInEasing.transform(homeStickyProgress)
            val pillScale = androidx.compose.ui.util.lerp(0.97f, 1f, frostShift)
            // v27v — the resting pills follow the HERO TINT (hoisted at the
            // top of the screen): when "Hero tint too" is on, the menu +
            // profile pills wear the tinted accent + on-accent ink.
            val heroPillBg = heroFill
            val heroPillIcon = questInk
            val heroPillRim = lerp(heroPillBg, heroPillIcon, 0.42f)
            // Both morph endpoints are fully opaque. The old hero endpoint
            // used a translucent ink wash, which let the banner show through
            // the pills and made them read like circular visual artifacts.
            // v81 — dark: the scrolled frosted pills become dark glass with
            // a light rim + light icon (the exact light-mode reversal).
            val frostBg = if (isCurioDarkTheme()) Color(0xFF1B1B1D) else Color.White
            val frostRim = if (isCurioDarkTheme()) Color(0xFF3A3A3E) else Color(0xFFD9DEE6)
            val frostIcon = if (isCurioDarkTheme()) MaterialTheme.colorScheme.onBackground
                            else homeReadableInk(frostBg)
            // v230 — LIQUID-GLASS MORPH ENDPOINT: when the experiment is on,
            // the scrolled pills become real liquid-glass capsules instead of
            // the flat frosted plate. At rest BOTH paths show the exact same
            // SOLID hero fill, so the resting look never changes. The profile
            // pill keeps the classic morph while an avatar photo is set (a
            // photo can't sit on glass) — so menu and profile animate their
            // fills independently.
            // v241 — GLASS HANDOFF RESTORED through the SAFE architecture:
            // the pills sample the LOCAL capture Box above (which contains
            // only what sits BEHIND them — the pills are a sibling overlay,
            // exactly the bottom-nav arrangement), so the old whole-page
            // self-capture cycle is impossible by construction. Fully CLEAR
            // glass (alwaysClear) with real refraction, per the request.
            val glassOn = isInScreenGlassActive()
            val profileAvatarPath = AppPreferences.profileAvatarPathState
            val profileGlassOn = glassOn && profileAvatarPath.isNullOrBlank()
            // Resolve solid target colors from scroll, then animate the paint
            // itself. The short tween gives a true color fade without adding
            // another geometric transition or ripple-like flash.
            // v267 — ALWAYS GLASS (user request): with liquid glass on, the
            // pills wear NO hero fill at all — they are clear refracting
            // glass from rest, not a solid hero shade that morphs away. The
            // dark mid-scroll state is gone because there is no solid phase
            // left to hand off from. The non-glass path keeps its classic
            // hero-fill → frost morph unchanged.
            val targetMenuBg = if (glassOn) Color.Transparent
                else lerp(heroPillBg, frostBg, frostShift)
            val targetProfileBg = if (profileGlassOn) Color.Transparent
                else lerp(heroPillBg, frostBg, frostShift)
            // v267 — always-glass: ink + rim sit at their scrolled values
            // from rest (readable black/white over clear glass), no morph.
            val targetPillRim = if (glassOn) frostRim else lerp(heroPillRim, frostRim, frostShift)
            val targetPillIcon = if (glassOn) frostIcon else lerp(heroPillIcon, frostIcon, frostShift)
            val menuPillBg by animateColorAsState(
                targetValue = targetMenuBg,
                animationSpec = tween(CurioMotion.Durations.Quick),
                label = "homeStickyMenuBackground"
            )
            val profilePillBg by animateColorAsState(
                targetValue = targetProfileBg,
                animationSpec = tween(CurioMotion.Durations.Quick),
                label = "homeStickyProfileBackground"
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
            // v246 — one gesture stream per pill, shared by the click and
            // the liquid-glass press feel (shrink + refraction bloom).
            val menuPillInteraction = remember { MutableInteractionSource() }
            val avatarPillInteraction = remember { MutableInteractionSource() }
            // v3xx22 — the glass style's avatar pill (photo or Person glyph),
            // reused by the morph bar's full row and compact row.
            val morphAvatar: @Composable (Color) -> Unit = { aInk ->
                TopBarPill(
                    onClick = { navController.navigate(CurioRoutes.PROFILE) { launchSingleTop = true } },
                    glyph = CurioIcons.Person,
                    contentDescription = "Profile",
                    shape = CircleShape,
                    bg = if (isCurioDarkTheme()) Color(0xFF1B1B1D) else Color.White,
                    rim = pillRim,
                    iconTint = aInk,
                    elevation = 3.dp,
                    pillInteraction = avatarPillInteraction,
                    avatarPath = profileAvatarPath,
                    showingPicture = true
                )
            }
            if (homeGlassOn) {
                CurioGlassToolbarMorph(
                    progress = homeStickyProgress,
                    compactHeight = HomeCompactHeaderHeight,
                    title = homeGreeting(),
                    subtitle = displayName,
                    compactTitle = "${homeGreeting()} $displayName",
                    onMenuClick = { CurioDrawerState.requestOpen() },
                    trailing = morphAvatar,
                    compactAvatar = { morphAvatar(questInk) },
                    // v3xx22 — the stats row (Streak · Cabinet · Topics)
                    // rides the FULL bar and fades out on collapse, so the
                    // slim compact bar reads as just the greeting.
                    content = { ink ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeroStatSegment(
                                glyph = "local_fire_department",
                                value = "$streakDays",
                                label = "Streak",
                                tint = ink,
                                ink = ink,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate(CurioRoutes.QUESTS) { launchSingleTop = true } }
                            )
                            VerticalDivider(
                                modifier = Modifier.height(34.dp),
                                color = ink.copy(alpha = 0.22f)
                            )
                            HeroStatSegment(
                                glyph = CurioIcons.Inventory2,
                                value = "$totalSaved",
                                label = "Cabinet",
                                tint = ink,
                                ink = ink,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigateToTab(CurioRoutes.CABINET) }
                            )
                            VerticalDivider(
                                modifier = Modifier.height(34.dp),
                                color = ink.copy(alpha = 0.22f)
                            )
                            HeroStatSegment(
                                glyph = CurioIcons.AutoAwesome,
                                value = "$topicsTotal",
                                label = "Topics",
                                tint = ink,
                                ink = ink,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate(CurioRoutes.DATABASE) { launchSingleTop = true } }
                            )
                        }
                    },
                    glassBackdrop = homeGlassBackdrop,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            } else {
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
                    // v147 — the drawer lives at the NavHost root now: the
                    // hamburger just raises the request and the NavHost
                    // opens its DrawerState (the bar stays composed beneath
                    // the drawer instead of hiding).
                    onClick = { CurioDrawerState.requestOpen() },
                    glyph = CurioIcons.Menu,
                    contentDescription = "Open menu",
                    shape = RoundedCornerShape(50),
                    bg = menuPillBg,
                    rim = pillRim,
                    iconTint = pillIcon,
                    // v230 — glass draws its own soft shadow, so the Surface
                    // elevation drops when the morph hands off to it.
                    elevation = if (glassOn) 0.dp else 6.dp * frostShift,
                    pillInteraction = menuPillInteraction,
                    // v267 — ALWAYS liquid glass (no solid phase to hand off
                    // from — see the always-glass note above).
                    modifier = if (glassOn)
                        Modifier.liquidGlassCapsule(
                            heroPillBg,
                            washAlpha = 0.45f,
                            backdrop = homeGlassBackdrop,
                            alwaysClear = true,
                            interactionSource = menuPillInteraction
                        ) else Modifier
                )
                TopBarPill(
                    onClick = { navController.navigate(CurioRoutes.PROFILE) { launchSingleTop = true } },
                    glyph = CurioIcons.Person,
                    contentDescription = "Profile",
                    shape = CircleShape,
                    bg = profilePillBg,
                    rim = pillRim,
                    iconTint = pillIcon,
                    elevation = if (profileGlassOn) 0.dp else 6.dp * frostShift,
                    pillInteraction = avatarPillInteraction,
                    // v267 — always glass while NO avatar photo is set; the
                    // photo keeps the classic frosted morph underneath it.
                    modifier = if (profileGlassOn)
                        Modifier.liquidGlassCapsule(
                            heroPillBg,
                            washAlpha = 0.45f,
                            backdrop = homeGlassBackdrop,
                            alwaysClear = true,
                            interactionSource = avatarPillInteraction
                        ) else Modifier,
                    // v118 — the profile pill wears the avatar photo when
                    // one is set (fresh pref read each composition, like the
                    // drawer) and falls back to the Person glyph otherwise.
                    // v443 — and it is the one pill that may wear a face.
                    avatarPath = profileAvatarPath,
                    showingPicture = true
                )
            }
            } // v3xx22 — end of the glass-morph-or-floating-pills branch
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
        // v407 — the hero's mirrored glyph collage tones down with the page
        // backdrop and the mood board (Appearance → "Glyph backdrop").
        tint = tint.copy(alpha = alpha * glyphWatermarkDepthScale()),
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
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    // Colored icon accent, extra-bold value, soft label — mirrors
    // EntryDetail's FrostedSegment, with the icon wearing the color accent.
    Column(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
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
    elevation: Dp,
    // v230 — optional outer modifier carrying the liquid-glass capsule
    // (drawBackdrop) that replaces the flat frost once scrolled.
    modifier: Modifier = Modifier,
    // v118 — when set, the avatar photo replaces the glyph; the pill's
    // animated rim still draws on top so the frosted scroll morph reads.
    avatarPath: String? = null,
    /**
     * v443 — WHETHER THIS PILL *IS* THE MEMBER'S OWN PICTURE.
     *
     * The path above is NOT the test, and treating it as one was the bug: a member
     * wearing their **blob** as their picture has no path at all
     * ([AppPreferences.profileAvatarBlobState]), so `hasOwnPicture(null)` is TRUE for
     * them and a pill that was handed no path — the drawer's hamburger — drew their
     * face instead of its own glyph. The member: *"the drawer menu icon on home
     * screen is getting the profile pic"*. So the pill SAYS which of the two it is,
     * and only the picture pill ever resolves a face (its own glyph is the fallback
     * for a member who has neither).
     */
    showingPicture: Boolean = false,
    // v246 — optional external gesture source: when the caller also wires
    // liquid-glass press feel, both must read the SAME stream.
    pillInteraction: MutableInteractionSource? = null
) {
    val fallbackInteraction = remember { MutableInteractionSource() }
    val interactionSource = pillInteraction ?: fallbackInteraction
    Surface(
        shape = shape,
        color = bg,
        shadowElevation = elevation,
        modifier = modifier
            // v244 — 44dp (was 42): the glyph's line box centers reliably in
            // the larger circle at every system font size.
            .size(44.dp)
            // v28 — dark mode elevation visibility (glow + hairline).
            .curioDarkGlow(elevation, shape)
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
            // v439 — their blob while they are wearing it, else their photo
            // (see [CurioMemberAvatar]): the drawer hero must show the same
            // picture the profile does.
            // v443 — AND ONLY THE PICTURE PILL ASKS (see [showingPicture]).
            if (showingPicture && hasOwnPicture(avatarPath)) {
                // The picture fills the pill (the Surface clips to the shape);
                // the rim ring rides on top so the pill keeps its frosted-rim
                // look while scrolling.
                CurioMemberAvatar(
                    avatarPath,
                    Modifier
                        .fillMaxSize()
                        // Keep the button's label when the glyph is hidden.
                        .semantics { this.contentDescription = contentDescription }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.5.dp, rim, shape)
                )
            } else {
                CurioIcon(
                    name = glyph,
                    contentDescription = contentDescription,
                    tint = iconTint,
                    size = 22.dp,
                    // The shared icon renderer centers the ink in the natural
                    // line box, but the menu/person glyphs' optical weight still
                    // reads a hair low inside the small 42dp pill — nudge it up
                    // (v115: deepened -0.5dp -> -1.5dp -> -2dp — the glyphs were
                    // still a touch low after the v114 centering fix).
                    // v233 — proportional nudge: stays centered at every font scale.
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Quest block — the big solid Shuffle CTA that lives between the hero
// tear and the content below.
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun QuestShuffleCard(
    /** The Shuffle disc's fill — a paper-white plate on the rose banner. */
    plate: Color,
    /** The glyph ink ON that disc (the banner's own readable ink). */
    ink: Color,
    /** The eyebrow's and the title's ink. */
    copyInk: Color,
    pet: (@Composable () -> Unit)? = null,
    onShuffle: () -> Unit,
    // v8.25 — the tour's home landmark modifier (bounds tracking only).
    modifier: Modifier = Modifier
) {
    // v7.32 — the quest is backgroundless: bare text + the shuffle button
    // sitting on the page (no card fill, no leading icon). The whole row
    // stays tappable so a tap on the copy shuffles too.
    // v411 — the quest moved INSIDE the torn banner, so every colour is
    // passed in: the caller owns the surface, and nothing here reaches for
    // `onSurface` any more (a dark plum title read as a stain on the rose
    // banner). The plate/ink pair is the one thing that had to invert — a
    // pastel-rose disc would vanish into its own hero.
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
                // v411 — no horizontal padding of its own: the row is inside
                // the banner column, which already insets 20dp, so the quest
                // copy lines up with the greeting and the name above it.
                .padding(vertical = 10.dp),
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
                // v411 — a bigger, cleaner hierarchy ("todays quest etc font
                // hirarcy more better bigger cleaner"): the eyebrow is a quiet
                // letterspaced label and the title carries the weight —
                // labelLarge + headlineMedium. Both lines wear the FULL copy
                // ink (no transparency); the size gap is what separates them.
                Text(
                    text = "TODAY'S QUEST",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    ),
                    color = copyInk
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Shuffle the deck",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp
                    ),
                    color = copyInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // v411 — "A fresh mix of ideas, picked for you" is GONE (the
                // member's call). The title already says what the button does;
                // the line was a label explaining a label.
            }
            Surface(
                shape = CircleShape,
                color = plate,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CurioIcon(
                        CurioIcons.Casino,
                        "Shuffle a random deck",
                        tint = ink,
                        size = 26.dp,
                        // The shared icon renderer already applies the
                        // standard 1dp optical lift; this extra half-dp is
                        // only for the casino glyph's heavier visible base.
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
            // v3xx46 — the shared press language (squish + one light tick).
            .curioPressClickable(pressedScale = 0.975f, onClick = onClick)
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
            // v3xx46 — same press feedback as the Saved row beside it.
            .curioPressClickable(pressedScale = 0.975f, onClick = onClick)
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
@OptIn(ExperimentalFoundationApi::class)
private fun RecentEntryRow(
    entry: CurioEntry,
    onClick: () -> Unit
) {
    // v455 — no `hold`: Home's recents are tap-only (see the note at the
    // recents preview). The hold menu lives on the Recents page's own rows.
    val cat = CurioCategories.byId(entry.topic.categoryId)
    // Solid category-tinted card in light mode — matches the recents topic
    // rows. v115 — dark mode: the Home recents go back to plain dark
    // surface cards (the category tint on pitch black was dropped); the
    // recents page (RecentScreen) keeps its tinted rows.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // v27u — recents rows sit on a soft 2dp lift; the white catch
            // stays at the TOP EDGE only (curioGlassEdge) — the full-pill
            // inner glow is gone.
            .curioGlassEdge(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isCurioDarkTheme()) MaterialTheme.colorScheme.surfaceContainerLow else cat.categorySurface(),
        shadowElevation = 2.dp
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

// ════════════════���══════════════════════════════════════════════════════
// First-time empty state
// ═══════════════════════════════════════════════════════���═══════════════

/**
 * The Home accent, resolved like the hero banner: the muted rose-wood base
 * normally, its airy pastel twin when pastel mode (the shipped default) is
 * on — so the hero, empty state and drawer all wear the SAME rose-wood.
 */
@Composable
private fun homeReadableInk(fill: Color): Color {
    // v420 — the ink on a named theme's hero is its own onPrimary pair.
    activeNamedTheme()?.let { return MaterialTheme.colorScheme.onPrimary }
    // v223 — Material hero tears: readable ink on primaryContainer.
    if (materialHeroTearsOn()) return MaterialTheme.colorScheme.onPrimaryContainer
    // v32 — when the shared hero wears the SPIN LANE's accent (Adaptive
    // Hero), the text must be accent-aware: white/cream on the deep accent
    // (never the fixed dark onSurface, which was invisible on a vivid lane
    // banner in non-pastel). The lane branch resolves like every category
    // hero ([heroHeaderInk]); the plain rose keeps the old ink.
    heroLaneCategory()?.let { return it.heroHeaderInk() }
    // v81 — dark mode: crisp light ink on the dark rose banner.
    if (isCurioDarkTheme()) return MaterialTheme.colorScheme.onBackground
    return if (!AppPreferences.pastelColorsState) MaterialTheme.colorScheme.onSurface
           else pastelFillInk(fill)
}

@Composable
private fun homeRoseAccent(): Color {
    // v420 — a named theme IS the hero: its own primary fill answers first.
    activeNamedTheme()?.let { return MaterialTheme.colorScheme.primary }
    // v223 — "Material hero tears": when the Material theme AND this
    // option are both on, the torn hero wears the scheme's
    // primaryContainer instead of the app-default rose/azure (or a lane).
    if (materialHeroTearsOn()) return MaterialTheme.colorScheme.primaryContainer
    // v30 — "Hero follows Spin lane": Home's shared hero wears the Spin
    // lane's accent too (the drawer + hero share this resolver).
    heroLaneCategory()?.let { cat -> return cat.headerAccent() }
    // v81 — dark mode: the torn hero wears a NEW SHADE of the same spectrum
    // — the deep rose/azure twins (never the light shade).
    if (isCurioDarkTheme()) {
        if (AppPreferences.heroBlueState) return CurioColors.HomeAzureDark
        val base = toHsl(CurioColors.HomeRosewood)
        if (AppPreferences.pastelColorsState) {
            val pinkHue = (base.h - 15f + 360f) % 360f
            // v421 — MUTED (see settingsRoseAccent): the dark rose banner was
            // carrying the light hero's vibrancy at a night lightness, which on
            // a black page read as neon.
            return fromHsl(
                pinkHue,
                (base.s * 0.62f).coerceIn(0f, 0.52f),
                0.40f
            )
        }
        return CurioColors.HomeRosewoodDark
    }
    // v27l — optional sky-azure hero: when enabled, the shared hero wears
    // the airy pastel azure (Science/Sky twin) instead of the rose-wood.
    if (AppPreferences.heroBlueState) {
        return CurioColors.HomeAzure
    }
    val base = toHsl(CurioColors.HomeRosewood)
    return if (AppPreferences.pastelColorsState) {
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
                // v474 — no instruction: the two buttons under this line ARE
                // the instruction ("Pick a lane" / "Surprise me"), and the quest
                // card above already says "Shuffle the deck". This says what the
                // area is for.
                "Captures from your first topic land here.",
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
                            // v81 — dark: the deep rose pill needs the bright
                            // light twin ink (deep plum would vanish).
                            tint = if (isCurioDarkTheme()) CurioColors.CoralBlush else CurioColors.DeepPlum,
                            size = 16.dp,
                            // Match the shared icon lift plus the casino
                            // glyph's half-dp extra correction.
                        )
                        Text(
                            "Surprise me",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isCurioDarkTheme()) CurioColors.CoralBlush else CurioColors.DeepPlum
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
                    shadowElevation = 2.dp,
                    // v28 — dark mode elevation visibility.
                    modifier = Modifier
                        .curioDarkGlow(2.dp, RoundedCornerShape(50))
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
// v3xx22 — the drawer hero's resting footprint in the GLASS toolbar style:
// the rounded-bottom glass bar (status bar + pill row + greeting block).
private val DrawerGlassHeroHeight = 140.dp

// v147 — the drawer now renders from the NavHost root (above the floating
// pill bar), so its content is called from CurioNavHost: internal instead
// of private. Its panel helpers below stay private.
@Composable
internal fun HomeDrawerContent(
    onNavigate: (String) -> Unit,
    /**
     * v444 — WHETHER THE DRAWER IS OPEN, so the sky can arrive and leave with it.
     *
     * The member: *"animate every time it closes and opens, with beautiful mesh
     * like animation, don't change the design, just beautifully animate it"*. The
     * star map is composed with the drawer, so without this it had no way to know
     * that the drawer was on its way OUT — its own light-up played on the way in
     * and then the whole panel simply slid off. The drawer's own state is the one
     * thing that knows both halves (see `CurioNavHost`).
     */
    drawerOpen: Boolean = true
) {
    val context = LocalContext.current
    val displayName = AppPreferences.displayNameState
    // v174 — the drawer hero becomes a dreamy pre-dawn sky instead of the
    // rose banner: pale seafoam gradient in light, deep twilight teal in
    // dark. heroFill/drawerInk keep the sky's base + readable ink so the
    // decoration layers below work untouched.
    val (skyTop, skyBottom, skyInk) = drawerSkyColors()
    val heroFill = skyTop
    val drawerInk = skyInk
    val heroTornShape = remember(HOME_DRAWER_TEAR_SEED) {
        SoftTornBottomShape(HOME_DRAWER_TEAR_SEED, bold = true)
    }
    val sheetShape = remember(HOME_DRAWER_TEAR_SEED) {
        SoftTornSheetShape(HOME_DRAWER_TEAR_SEED, lip = 10.dp, baseline = 14.dp, bold = true)
    }

    ModalDrawerSheet(
        // v409 — the width the lane grid is laid out for: four 70dp tiles
        // with 8dp gutters inside the 16dp content padding, which is what
        // keeps the grid readable without a horizontal scroll.
        modifier = Modifier.width(336.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        // The hero banner tears from the very top edge — run the sheet
        // content up behind the status bar (the hero draws its own
        // top spacing, and the footer adds its own nav-bar inset).
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // -- The brain panel - drawn first so it scrolls UNDER the tear --
            // v206 — the panel sits above the footer, and the footer is the
            // LazyColumn's last item so it scrolls with the content instead
            // of pinning over it.
            // v3xx22 — the glass style's drawer hero is the shorter rounded
            // glass bar, so the rows' top clearance is style-aware.
            val drawerGlassOn = AppPreferences.headerStyleState == AppPreferences.HeaderStyle.GLASS
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (drawerGlassOn) DrawerGlassHeroHeight + 14.dp
                          else HomeDrawerHeroHeight + HomeDrawerSheetExtent + 14.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // v409 — THE DRAWER IS THE BRAIN. One panel: the member's
                // real numbers over the lane grid, built from real UI (see
                // DrawerBrainPanel).
                item("brain") {
                    DrawerBrainPanel(
                        onOpenStats = { onNavigate(CurioRoutes.STATS) },
                        drawerOpen = drawerOpen
                    )
                }
            // v206 — the footer scrolls with the list as the last item.
            item("footer") {
                DrawerFooter()
            }
            }

            // -- Drawer hero — the GLASS toolbar bar (style on) or the torn
            // celestial sky banner (rows vanish at the seam) --------------
            if (drawerGlassOn) {
                DrawerGlassHero(displayName = displayName, drawerOpen = drawerOpen)
            } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HomeDrawerHeroHeight + HomeDrawerSheetExtent)
            ) {
                // Paper under-sheet (same seed -> pixel-aligned seam). v81 —
                // dark: a subtle lighter lip so the seam reads on the dark
                // banner.
                // v108 — OFF by default (the hero tears straight into the
                // page); the Experiments toggle restores this layer.
                if (AppPreferences.heroTearSheetState) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .offset(y = HomeDrawerHeroHeight - 18.dp)
                        .clip(sheetShape)
                        .background(
                            if (isCurioDarkTheme()) lerp(heroFill, Color.White, 0.10f)
                            else CurioColors.CreamWhite
                        )
                )
                }
                // Torn-edge shadow - hairline rim under the ragged seam.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HomeDrawerHeroHeight)
                        .offset(y = 1.dp)
                        .clip(heroTornShape)
                        .background(Color.Black.copy(alpha = 0.20f))
                )
                // Celestial sky banner with the bold torn bottom edge —
                // v175: the hero's sky is the uploaded SVG artwork (the dark
                // night sky in dark theme, the day sky in light), clipped to
                // the torn banner via Coil's SvgDecoder — exactly like the
                // drawer footer's landscape. The theme gradient sits behind
                // it as the loading backdrop; the greeting reads as the
                // sky's scenery. No watermark glyphs on the banner.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HomeDrawerHeroHeight)
                        .clip(heroTornShape)
                        .background(Brush.verticalGradient(listOf(skyTop, skyBottom)))
                ) {
                    // v175 — theme-picked hero sky SVG (night sky in dark,
                    // day sky in light).
                    val heroSkyRes = if (isCurioDarkTheme()) R.raw.drawer_hero_sky_dark else R.raw.drawer_hero_sky_light
                    val heroSkyModel = remember(context, heroSkyRes) {
                        ImageRequest.Builder(context)
                            .data(heroSkyRes)
                            .decoderFactory(SvgDecoder.Factory())
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = heroSkyModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // v177 — tap the moon (dark sky) / sun (light sky) to
                    // flip the theme. The artwork is exactly 320x186dp —
                    // 1:1 with the hero box — and both celestial bodies sit
                    // at (268.8, 52.08) in their SVG, so a 48dp invisible
                    // hit-circle there toggles the theme mode directly.
                    // Always-on per the user (no Settings toggle).
                    val sunMoonTap = 48.dp
                    // v181 — resolve the theme in COMPOSITION: isCurioDarkTheme
                    // is @Composable and can't run inside the clickable lambda
                    // (CI caught it at 2132).
                    val isDarkNow = isCurioDarkTheme()
                    // v(theme switch) — the quick flip plays a Telegram-style
                    // circular reveal from the sun/moon itself. The transition
                    // + coroutine scope are resolved in composition (clickable
                    // isn't @Composable); bounds give the window-space origin.
                    val themeTransition = LocalCurioThemeTransition.current
                    val transitionScope = rememberCoroutineScope()
                    var sunMoonBounds by remember {
                        mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = 268.8.dp - sunMoonTap / 2, y = 52.08.dp - sunMoonTap / 2)
                            .size(sunMoonTap)
                            .onGloballyPositioned { coords -> sunMoonBounds = coords.boundsInWindow() }
                            .clip(CircleShape)
                            .clickable {
                                switchThemeWithReveal(
                                    transition = themeTransition,
                                    scope = transitionScope,
                                    context = context,
                                    center = sunMoonBounds.takeIf { it != Rect.Zero }?.center ?: Offset.Zero,
                                    newMode = if (isDarkNow) AppPreferences.THEME_LIGHT
                                              else AppPreferences.THEME_DARK,
                                )
                            }
                    )
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Brand + greeting (with the profile avatar) pinned
                        // just above the tear. v103 — the avatar photo (or the
                        // name initial) shows here too, matching the Profile
                        // hero; a fresh pref read keeps it in sync.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
                        ) {
                            val avatarPath = AppPreferences.profileAvatarPathState
                            // v118 — the avatar grew 48 → 56dp and the
                            // greeting text stepped up (CURIO labelMedium,
                            // name headlineMedium, tagline bodyMedium) per
                            // the user's "a little bigger" request.
                            // v122 — 56 → 64dp, the row sits a touch higher
                            // (bottom 28 → 40dp), and a long name auto-shrinks
                            // to fit instead of being cut.
                            // v174 — a cream ring so the avatar pops against
                            // the sky; the fallback initial uses the deep
                            // seafoam ink (readable on the cream in BOTH
                            // themes).
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .shadow(2.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(CurioColors.CreamWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasOwnPicture(avatarPath)) {
                                    CurioMemberAvatar(avatarPath, Modifier.fillMaxSize())
                                } else {
                                    Text(
                                        displayName.firstOrNull()?.uppercase().orEmpty(),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color(0xFF2C5A53)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "CURIO",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = drawerInk.copy(alpha = 0.85f)
                                )
                                // v122 — a long name steps the font down so
                                // it fits the row instead of getting cut: the
                                // greeting grows past headlineMedium and the
                                // style drops to titleLarge, then titleMedium
                                // (the single-line ellipsis stays as the last
                                // resort). The manual steps avoid the
                                // TextAutoSize API, which isn't resolvable on
                                // this project's Compose classpath.
                                // v123 — the "Spin it. Explore it. Capture
                                // it." tagline is GONE: the first name stays
                                // in the greeting's position, and the middle +
                                // last names fill the tagline's spot at the
                                // tagline's size (bodyMedium).
                                // v134 — the rest of the name (middle + last)
                                // reads on ONE line below the greeting, not
                                // one line per name part.
                                val nameParts = displayName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                                val firstName = nameParts.firstOrNull() ?: displayName
                                val greeting = "Hi $firstName"
                                val greetingStyle = when {
                                    greeting.length <= 16 -> MaterialTheme.typography.headlineMedium
                                    greeting.length <= 26 -> MaterialTheme.typography.titleLarge
                                    else -> MaterialTheme.typography.titleMedium
                                }
                                Text(
                                    greeting,
                                    style = greetingStyle.copy(fontWeight = FontWeight.ExtraBold),
                                    color = drawerInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val restOfName = nameParts.drop(1).joinToString(" ")
                                val bio = AppPreferences.getCustomStreakTagline(context)
                                val subtitle = restOfName.ifBlank { bio }
                                if (subtitle.isNotBlank()) {
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = drawerInk.copy(alpha = 0.78f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            } // v3xx22 — end of the glass-or-torn drawer hero branch
        }
    }
}

/** v3xx22 — the drawer hero when the app-wide GLASS toolbar header style is
 *  on: the same rose-tinted glass bar as the app headers — rounded bottom
 *  curve, its own frost — holding the avatar + CURIO greeting block and the
 *  sun/moon theme-flip pill (the sky hero's toggle, restyled as a glass
 *  pill). The menu rows scroll beneath it exactly like the torn hero. */
@Composable
private fun DrawerGlassHero(
    displayName: String,
    /** v444 — the drawer's own state, so the sky plays in with it and out with it. */
    drawerOpen: Boolean = true
) {
    val context = LocalContext.current
    val dark = isCurioDarkTheme()
    val rose = settingsRoseAccent()
    val container = lerp(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        rose,
        if (dark) 0.14f else 0.20f
    )
    val ink = MaterialTheme.colorScheme.onSurface
    val pillBg = if (dark) lerp(container, Color.Black, 0.15f)
    else lerp(container, curioPillTintLift(), 0.38f)
    val avatarPath = AppPreferences.profileAvatarPathState
    // v(theme switch) — the quick flip plays a Telegram-style circular
    // reveal from the sun/moon pill itself.
    val isDarkNow = isCurioDarkTheme()
    val themeTransition = LocalCurioThemeTransition.current
    val transitionScope = rememberCoroutineScope()
    var sunMoonBounds by remember {
        mutableStateOf(Rect.Zero)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DrawerGlassHeroHeight)
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(container.copy(alpha = 0.96f))
            .statusBarsPadding()
    ) {
        // Sun/moon theme-flip pill — top end.
        Surface(
            onClick = {
                switchThemeWithReveal(
                    transition = themeTransition,
                    scope = transitionScope,
                    context = context,
                    center = sunMoonBounds.takeIf { it != Rect.Zero }?.center ?: Offset.Zero,
                    newMode = if (isDarkNow) AppPreferences.THEME_LIGHT
                              else AppPreferences.THEME_DARK,
                )
            },
            shape = CircleShape,
            color = pillBg,
            contentColor = ink,
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 14.dp)
                .size(40.dp)
                .onGloballyPositioned { coords -> sunMoonBounds = coords.boundsInWindow() }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CurioIcon(
                    name = if (isDarkNow) CurioIcons.LightMode else CurioIcons.DarkMode,
                    contentDescription = "Toggle theme",
                    size = 18.dp,
                    tint = ink
                )
            }
        }
        // Avatar + CURIO greeting block — bottom start (the sky hero's row).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(CurioColors.CreamWhite),
                contentAlignment = Alignment.Center
            ) {
                if (hasOwnPicture(avatarPath)) {
                    CurioMemberAvatar(avatarPath, Modifier.fillMaxSize())
                } else {
                    Text(
                        displayName.firstOrNull()?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF2C5A53)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "CURIO",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    ),
                    color = ink.copy(alpha = 0.85f)
                )
                val nameParts = displayName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                val firstName = nameParts.firstOrNull() ?: displayName
                val greeting = "Hi $firstName"
                val greetingStyle = when {
                    greeting.length <= 16 -> MaterialTheme.typography.headlineMedium
                    greeting.length <= 26 -> MaterialTheme.typography.titleLarge
                    else -> MaterialTheme.typography.titleMedium
                }
                Text(
                    greeting,
                    style = greetingStyle.copy(fontWeight = FontWeight.ExtraBold),
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val restOfName = nameParts.drop(1).joinToString(" ")
                val bio = AppPreferences.getCustomStreakTagline(context)
                val subtitle = restOfName.ifBlank { bio }
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * v409 — THE DRAWER'S BRAIN PANEL.
 *
 * The drawer's row menu is gone (Quests & Levels, the collapsible "Your
 * Curiosity" group, "About") — every one of those doors already exists one tap
 * away, and the drawer was carrying three navigation trees underneath a stats
 * map. What is left is what a drawer can only do here: the member's REAL
 * numbers (streak, level, knowledge) and the lane grid, which is the same
 * [CurioLaneGrid] the Stats page uses.
 *
 * Every element is real UI — cards, icons, meters, a ripple on tap, an
 * animated ring on the selected lane. Nothing is drawn (the old painted
 * constellation is gone; see CurioLaneGrid).
 *
 * The "YOUR BRAIN" row is the panel's ONE door to the full Stats page; the
 * tiles below only select, and the selection shows its own line under the
 * grid.
 */
@Composable
private fun DrawerBrainPanel(
    onOpenStats: () -> Unit,
    /**
     * v448 — the drawer's own state, handed down from [HomeDrawerContent].
     *
     * The star map is drawn at the end of THIS panel's body, so this is the flag
     * that has to carry the drawer's state to it: `open` was not in scope here at
     * all (the build had been failing on that line since v444), and the flag is
     * named `drawerOpen` rather than `open` because `open` is a MODIFIER KEYWORD
     * and `open = open` reads as a modifier in expression position.
     */
    drawerOpen: Boolean = true
) {
    val context = LocalContext.current
    val streak = remember(context) { StreakTracker.getStreak(context) }
    val xp = CurioQuests.xpState
    val level = CurioQuests.levelForXp(xp)
    val (levelProgress, nextThreshold) = CurioQuests.xpProgress(xp)
    val progress = remember(context) { CurioPassport.allProgress(context) }
    // v457 — and the drawer's own read is seeded the same way, so the knowledge
    // map is drawn from the member's real archive on its first frame instead of
    // coming up dark and lighting a beat later (see [HomeFeedSnapshot]).
    val entries by produceState(initialValue = HomeFeedSnapshot.entries) {
        val loaded = runCatching { CurioRepositoryHolder.repo.getAll() }.getOrNull() ?: return@produceState
        HomeFeedSnapshot.entries = loaded
        value = loaded
    }
    val knowledge = remember(progress, entries) { laneKnowledge(progress, entries) }
    // Derived in composition (NOT remembered) so hiding a lane in Manage
    // Categories, reordering the lanes, or a theme flip re-reads the grid.
    val lanes = laneGridItems(knowledge)
    // v411 — the map's own order: the member's CATALOG order, not the grid's
    // explored-first one. A star must never move because a different lane's
    // knowledge changed.
    val mapLanes = lanes.sortedBy { it.id }
    val totalKnowledge = lanes.sumOf { it.knowledge }
    var selected by remember { mutableStateOf<CategoryId?>(null) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            onClick = onOpenStats,
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "YOUR BRAIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        ),
                        color = muted,
                        modifier = Modifier.weight(1f)
                    )
                    CurioIcon(CurioIcons.ChevronRight, null, tint = muted, size = 18.dp)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DrawerBrainStat(
                        glyph = CurioIcons.LocalFire,
                        value = "$streak",
                        label = "day streak",
                        tint = Color(0xFFC96F4A),
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(modifier = Modifier.height(36.dp))
                    DrawerBrainStat(
                        glyph = CurioIcons.WorkspacePremium,
                        value = "Lv $level",
                        label = "level",
                        tint = Color(0xFFD9A85C),
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(modifier = Modifier.height(36.dp))
                    DrawerBrainStat(
                        glyph = CurioIcons.AutoAwesome,
                        value = "$totalKnowledge",
                        label = "knowledge",
                        tint = CurioColors.DustyBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { levelProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = Color(0xFFD9A85C),
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (level >= CurioQuests.maxLevel) "Top level reached"
                    else "${nextThreshold - xp} XP to level ${level + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted
                )
            }
        }
        // ── v411 — THE CURIOSITY MAP ─────────────────────────────────────
        // The lane GRID is gone from the drawer (the member's call: "why did u
        // add your lanes in drawer burh, remove it and do something else, with
        // the graph or something"). The lanes are a CONSTELLATION now — one
        // star per lane, its size and brightness its knowledge, its colour its
        // own accent. Nothing here is a progress chart.
        DrawerLaneStarMap(
            lanes = mapLanes,
            selected = selected,
            onSelect = { selected = it },
            drawerOpen = drawerOpen
        )
        // v427 — NO COUNTER UNDER THE SKY. A line reading "1 of 338 lanes
        // explored" under a map is a meter wearing a caption: the stars already
        // say who is lit and who is not (member: "remove the 1 out of 338 lanes
        // explored"). What is left under the map is the readout of the star you
        // actually tapped.
        val picked = lanes.firstOrNull { it.id == selected }
        if (picked != null) {
            // The readout NAMES the star that was tapped (the same strip the
            // Stats grid shows), so the map itself never has to shout 36
            // labels over the sky.
            CurioLaneDetailStrip(picked)
        }
    }
}

/**
 * v457 — WHAT HOME PAINTS ON ITS FIRST FRAME.
 *
 * The member: *"many ui elements have loading unloading behaviors
 * unnecessarily"*, and, asked which: *"screens that re-read everything when you
 * come back … the books and journals in home screen"*. Every read in this screen
 * that feeds a list started at `emptyList()`, so a return to Home dropped its
 * rows (and the drawer's knowledge map) to nothing and filled them in a frame or
 * two later — an unload and a load, on every visit, for an archive that had not
 * changed.
 *
 * One snapshot for the archive, read by both consumers (the feed's recents and
 * the drawer's own map), so the second one costs no extra read and the two can
 * never disagree about what the member has saved. `@Volatile` because the read
 * runs on a frame's coroutine and the next composition reads on the main thread
 * with no lock between them.
 */
private object HomeFeedSnapshot {
    @Volatile var entries: List<CurioEntry> = emptyList()
}

/**
 * How tall the drawer's constellation stands.
 *
 * v414 — it COVERS MORE OF THE DRAWER: 188dp made the chart read as a strip
 * wedged between the brain stats and the lane readout, so the sky grew to 254dp
 * and the stars now spread to the panel's own edges (member: "it doesnt cover
 * the drawer a little more").
 *
 * v422 — 320dp, because this number was the ONLY dial that could make the
 * pattern bigger. [starPoint] multiplied a polar radius by half the box's
 * SHORTER side — so widening the drawer, or the panel, moved not one star
 * (member: "in the drawer the pattern is good … incrase the size").
 *
 * v427 — 372dp, AND the field fills the box on both axes now (see [starPoint]).
 * The v422 note above was only half true: the drawer is a fixed 336dp wide, so
 * its map measures 304dp across, and the SHORTER side was the WIDTH — a taller
 * sky used to add empty paper above and below the stars and nothing else. With
 * the reach following each axis the extra height is real: the sky reads longer,
 * the stars spread into it, and the constellations run further (member: "a
 * little more longer look too").
 */
private val DrawerStarMapHeight = 372.dp

/**
 * v411 — THE DRAWER'S CURIOSITY MAP: the lanes as a sky.
 *
 * The member on the drawer's lane grid: "remove it and do something else, with
 * the graph or something" — and, on what that graph should be, "a unique graph
 * look with star style something, but not progress style graph". So the drawer
 * no longer lists lanes as tiles; it DRAWS them.
 *
 * Every lane is one star:
 *  * **Position** — fixed for a given lane count ([starScatterByFamily]: a
 *    golden-angle spiral with a small hashed wobble, deterministic and
 *    identical at every knowledge level, so the map is a landmark the member
 *    learns; v477 keeps that shape exactly and only pulls each family's lanes a
 *    little toward each other, so similar categories read as a soft
 *    neighbourhood). v419
 *    replaced the v414 ring-and-spoke lattice with this — the lattice was too
 *    symmetric (member: "the drawer graph is bad … its too symmetric") — and a
 *    faint dust field ([starDust]) gives the panel its depth instead of a grid.
 *  * **Light, not size** (v477) — every star's disc is the same size
 *    ([StarCoreDp]); knowledge shows as brightness and colour only. An explored
 *    lane is a LIT star in its own accent; an untouched one is a dim point, so
 *    the map shows the member what is left without being a meter — and without
 *    any dot growing fat as an archive fills (the member: *"dont … grow bigger
 *    when too much knowledge dont let them grow at all"*).
 *  * **Colour** — the lane's own accent, glowing through two halo steps.
 *  * **Hairlines** — each star joined to its NEAREST family-mate, and each
 *    family tied to its nearest other family by ONE longer hairline, so every
 *    branch is a loose constellation and the sky is still a single web
 *    ([starLinksGrouped], v476).
 *
 * It is TAPPABLE: the star nearest a touch within a 30dp halo is picked (the
 * tap target is the finger, not the dot) and the readout under the map names
 * the pick. Nothing on this canvas is transparent — every colour is an OPAQUE
 * mix between the SURFACE the sky is drawn on and the ink ([lerp]), which is
 * the member's rule for the new themes ("dont use transparent colors") applied
 * to the one surface in the app that is nothing but paint.
 *
 * The sky lights up ONCE, star by star ([Animatable], not an infinite
 * transition): the drawer is composed even while it is closed, so an idle
 * twinkle would spend the battery on a surface nobody is looking at.
 *
 * v422 — NO PLATE, A TALLER SKY, SOLID POINTS, AND A PICK THAT COMES ON.
 *
 *  * **The panel is GONE.** Its own fill, its corner radius and its clip are
 *    removed and the sky is drawn straight on the drawer's page (member: "now
 *    remove its background just keep the pattern"). Every tone in it therefore
 *    reads from `surface` — the page the map actually sits on — instead of from
 *    a plate that is no longer there, which is also what made the halos look
 *    like plate-tinted blobs in BOTH modes (member: "fix its colors in both
 *    white and dark mode just the draer").
 *  * **The sky grew** (254 → 320dp — see [DrawerStarMapHeight] for why the
 *    height is the only dial that can do it).
 *  * **An untouched lane is a SOLID point**, never a hollow ring: a sky is made
 *    of points (member: "make the dots of it solid filled").
 *  * **The picked star LIGHTS UP** — its core grows and its halo steps brighten
 *    toward the lane's own accent, and an untouched lane's solid point lights in
 *    its colour too, at the smallest lit size. The orbit ring is gone: it was
 *    the one piece of chrome the map added, and it read as a circle drawn around
 *    a star rather than as the star answering (member: "tapping them the
 *    highligh tis bad fix that too").
 */
@Composable
private fun DrawerLaneStarMap(
    lanes: List<LaneGridItem>,
    selected: CategoryId?,
    onSelect: (CategoryId?) -> Unit,
    /** v444 — the drawer's own state: the sky plays in with it and out with it. */
    drawerOpen: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (lanes.isEmpty()) return
    // v422 — THE PAGE, NOT A PLATE. The map used to carry its own
    // `surfaceContainerHigh` panel and every tone in the drawing was mixed FROM
    // that fill. The plate is gone, so the base is the surface the sky is drawn
    // on — the drawer's own `surface` — which is what keeps the stars, their
    // halos, the hairlines and the dust reading in white AND in dark mode.
    val page = MaterialTheme.colorScheme.surface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val dark = isCurioDarkTheme()
    // ── v476/477 — THE LANES GROUP BY FAMILY ──────
    // The member: *"kee the pattern same same dot connections group like similar
    // category in that branch"*. Similar categories already have a name in the
    // catalog — [CategoryFamily] (Artists · Albums · Songs are MUSIC, and so on)
    // — so similar lanes are linked to each other and each family keeps ONE link
    // to its nearest other family, so the sky is still a single web (the member:
    // "one connected sky"). v477 softens how hard that grouping MOVES the stars
    // (see [starScatterByFamily]) — the links stay, the herding does not.
    val familyOf: List<CategoryFamily> = remember(lanes) {
        lanes.map { CategoryFamily.of(it.id) }
    }
    val slots = remember(familyOf) { starScatterByFamily(familyOf) }
    val links = remember(slots, familyOf) { starLinksGrouped(slots, familyOf) }
    // Each family's own turn in the light-up (v476): the sky lights BRANCH BY
    // BRANCH rather than star by star (the member's own words).
    val familyOrder = remember(familyOf) { familyOf.distinct() }
    val slotFamily = remember(familyOf, familyOrder) { familyOf.map { familyOrder.indexOf(it) } }
    // v476 — where a lane sits INSIDE its own branch (0 = the family's first
    // lane, 1 = its last), so a branch can draw star-to-star instead of all at
    // once (see [bornOf]).
    val slotBranchRank: List<Float> = remember(slotFamily) {
        val totals = HashMap<Int, Int>()
        slotFamily.forEach { f -> totals[f] = (totals[f] ?: 0) + 1 }
        val seen = HashMap<Int, Int>()
        slotFamily.map { f ->
            val total = totals[f] ?: 1
            val i = seen[f] ?: 0
            seen[f] = i + 1
            if (total <= 1) 0f else i.toFloat() / (total - 1).toFloat()
        }
    }
    // A faint dust field behind the stars, so the panel has depth without the
    // regular grid the member rejected (v419).
    val dust = remember { starDust(STAR_DUST_COUNT) }
    val density = LocalDensity.current
    // The box, in pixels, for everything that has to be measured rather than drawn.
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    // ── v457 — WHERE EVERY STAR SITS, IN PIXELS ───────────────────────────
    //
    // Position, the tap target and the drawing all read ONE list now. It used to be
    // recomputed inside the canvas and again inside the hit test (the same maths,
    // written twice), and the halos below need it in COMPOSITION — a star's glow is
    // a cached brush, not something a draw pass can build. It is derived from the
    // measured size, so a resize moves every star with the box (see [starPoint]).
    val starPoints = remember(sizePx, slots) {
        if (sizePx.width <= 0 || sizePx.height <= 0) {
            emptyList()
        } else {
            val middle = Offset(sizePx.width / 2f, sizePx.height / 2f)
            slots.map { slot ->
                starPoint(slot, middle, sizePx.width.toFloat(), sizePx.height.toFloat())
            }
        }
    }
    // ── v457 — THE SKY HAS A LIGHT IN THE MIDDLE OF IT ────────────────────
    //
    // The member: *"make the star pattern animation more better and also more
    // noticeable shade of it, with a slight glowish background, not a solid box but
    // a slight glowish rounded or side rounded background"* — and, asked which
    // shape, **a radial glow from the centre**.
    //
    // So this is not the panel v422 removed coming back: there is no plate, no
    // edge, no corner radius to read as a box. It is ONE radial gradient centred on
    // the hub, from a warm whisper of the brand colour at the middle out to the
    // page's own colour at its rim — a lit sky with nothing drawn round it. Every
    // stop is OPAQUE and the last one IS the page, so it cannot catch the eye with
    // a seam (the v422 rule for this surface, which a gradient satisfies by
    // construction rather than by mixing).
    //
    // It is built once per measured size and drawn as one rect, and it rides the
    // same `reveal` layer as every star, so it arrives and leaves with the drawer.
    // v460 — AND ITS FALLOFF IS SMOOTH, AT A STRENGTH EACH THEME ASKS FOR.
    //
    // The member, of this sky: *"the drawer constellation i can see the edges in
    // dark mode, and also in light mode its not visible"*. Both halves were this
    // one gradient:
    //
    //  · **the edges** — four stops is a PIECEWISE-LINEAR ramp, and the eye reads
    //    every place its slope changes as a ring, which is exactly the banding a
    //    dark page makes easy to see: the wash arrived as concentric edges rather
    //    than as light ([glowStops] samples an eased curve at fine steps instead,
    //    and its slope reaches zero at the rim, so there is no boundary at the
    //    gradient's own radius either);
    //  · **not visible in light** — the base tint and the strength were the same
    //    numbers in both themes, and a 22% wash of the accent on a near-white
    //    page is not a glow, it is a slightly different white. The strength is
    //    the theme's own now: the light page asks for a deeper wash than the
    //    dark one, which is the opposite of how a shadow behaves and the whole
    //    reason this needed its own number.
    //
    // It is still built once per measured size and drawn as one rect (the extra
    // stops cost nothing at draw time — the brush is cached), and it still rides
    // the same `reveal` layer as every star.
    val glowTint = lerp(page, MaterialTheme.colorScheme.primary, GlowTintMix)
    // ── v477 — ONE WASH AGAIN ─────────────────────────────────────────────
    // v476 faded this single wash to make room for a glow per family; those
    // blooms are gone (they cost a dozen full-canvas gradients every frame and
    // bled over the panel above — see the canvas note), so the sky stands in the
    // one centre glow again, at its own full strength.
    val glowStrength = if (isCurioDarkTheme()) DarkGlowStrength else LightGlowStrength
    val centreGlow = remember(sizePx, page, glowTint, glowStrength) {
        if (sizePx.width <= 0 || sizePx.height <= 0) {
            null
        } else {
            Brush.radialGradient(
                colors = glowStops(page, glowTint, glowStrength),
                center = Offset(sizePx.width / 2f, sizePx.height / 2f),
                radius = maxOf(sizePx.width, sizePx.height) * GlowReach
            )
        }
    }
    // ── v457 — ONE BRUSH PER STAR, BUILT ONCE PER LOOK ─────────────────────
    //
    // The bloom used to be FOUR concentric circles per star, repainted every frame
    // of the twinkle: ~144 opaque discs on a 372dp canvas at 60fps, for a sky whose
    // stars only breathe. That is the cost the member felt as "clanky" and it is also
    // why the glow read as a dot with rings round it — three flat steps is not a
    // falloff. A radial gradient IS the falloff those steps were approximating, so
    // each star draws ONE disc now: a quarter of the ops, a genuinely smooth glow
    // (their *"make the glow better"*), and the twinkle and the arrival become a
    // canvas TRANSFORM over a brush that never changes (see the star pass).
    //
    // The colours are only slightly deeper than v444's steps, because the member
    // asked for a more noticeable SHADE, not for a brighter sky: the core stays the
    // lane's own accent, the mid ring is a touch stronger, and the outermost stop is
    // the PAGE's colour so the halo has no rim (the "circle drawn around a star"
    // look v444 was trying to lose). A picked star's aura is stronger still, which is
    // what makes the tap answer — the disc itself moves the same 18% it did.
    //
    // `null` means "not a lit star": an unexplored, unpicked lane stays the plain
    // solid point v422 settled on.
    val lanesKey = lanes.joinToString(",") { "${it.id.name}:${it.knowledge}:${it.explored}" }
    // ── v477 — NO PER-FAMILY BLOOMS ────────────────────────────────────────
    // v476 gave every family its own radial bloom (see the constants it left
    // behind). Each one was a full-canvas gradient circle drawn EVERY frame —
    // twelve of them on top of the wash and the stars, which is the lag — and
    // because a circle of that radius reaches past the map's own box with
    // nothing clipping it, the blooms painted up over the brain panel and left
    // the top of the drawer blank. The single centre wash ([centreGlow]) is the
    // sky's light again, and the canvas clips to its own box now, so nothing can
    // bleed over the panel.
    val starHalos: List<Pair<Float, Brush>?> = remember(
        lanesKey, starPoints, selected, page, ink, dark, density
    ) {
        lanes.mapIndexed { index, lane ->
            val picked = lane.id == selected
            // The point a star is drawn at: the SAME list the canvas and the tap
            // test use, so a halo can never be built around a place the star is not.
            val centre = starPoints.getOrNull(index) ?: return@mapIndexed null
            if (!lane.explored && !picked) return@mapIndexed null
            // ── v477 — EVERY DOT IS THE SAME SIZE ─────────────────────────
            // The core used to grow with the lane's knowledge (2.2dp for a fresh
            // lane → 5.6dp for a full one), which is exactly what the member
            // called out: *"dont make the dots glow too much or grow bigger when
            // too much knowledge dont let them grow at all"*. Knowledge shows as
            // COLOUR AND LIGHT only now — a lit lane wears its accent and its
            // halo, an untouched one is a dim point — and the DISC stays
            // [StarCoreDp] whatever the archive holds. The tap still opens the
            // star it answered (the picked bump), which is feedback, not a
            // measure.
            val core = StarCoreDp * (if (picked) 1.18f else 1f)
            val corePx = with(density) { core.dp.toPx() }
            // v476 — the star's own colour, deepened a touch on the light page
            // so a lit dot reads as a dot rather than as a slightly coloured
            // smudge (the member: *"the lines and the dots make it more
            // noticable, in light mode darker shade and similar"*).
            val accent = if (dark) lane.accent else lerp(lane.accent, ink, LightDotDarken)
            // v460 — the same eased falloff the sky's own wash uses, at the
            // star's strength: a star's aura was four stops too, so on the dark
            // page it carried the same visible rings the centre wash did (see
            // [glowStops]). The exponent is gentler here — a star keeps a solid
            // core and only its tail softens, which is what the four steps were
            // standing in for.
            corePx to Brush.radialGradient(
                colors = glowStops(
                    page = page,
                    tint = accent,
                    strength = if (picked) 0.99f else 0.95f,
                    power = 1.5f
                ),
                center = centre,
                radius = corePx * HALO_REACH
            )
        }
    }
    val lit = remember { Animatable(0f) }
    LaunchedEffect(lanes.size) {
        lit.snapTo(0f)
        // v476 — a longer sweep, because it now carries one BRANCH per turn
        // instead of one star per turn, and because each branch is DRAWN inside
        // its turn rather than switched on: at 900ms a family was on and off the
        // page before the eye could follow it (see [bornOf]).
        lit.animateTo(1f, tween(1320, easing = FastOutSlowInEasing))
    }
    // ── v444 — THE SKY ARRIVES AND LEAVES WITH THE DRAWER ─────────────
    //
    // The member: *"animate every time it closes and opens, with beautiful mesh
    // like animation don't change the design"*. One progress drives all of it:
    // the whole panel eases up a hair as it fades in, every hairline runs out from
    // its own star toward the one it joins (so the mesh DRAWS itself rather than
    // appearing finished), and the two are reversed together on the way out. The
    // design is untouched — same stars, same places, same colours.
    val reveal = remember { Animatable(if (drawerOpen) 1f else 0f) }
    LaunchedEffect(drawerOpen) {
        reveal.animateTo(
            targetValue = if (drawerOpen) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (drawerOpen) 760 else 300,
                easing = FastOutSlowInEasing
            )
        )
    }
    // ── v444 — AND IT TWINKLES WHILE IT IS UP ────────────────────────
    //
    // A sky nobody is looking at must not spend the battery: the clock only runs
    // while the drawer is OPEN, writes one float, and that float is read INSIDE the
    // draw block — so a twinkle costs draw passes and never a recomposition of the
    // map, its stars or the page behind it.
    var beat by remember { mutableStateOf(0f) }
    LaunchedEffect(drawerOpen) {
        // v454 — Lite mode parks the twinkle: an open sky that holds still is
        // the same drawing, it just stops asking for a frame every 16ms.
        if (!drawerOpen || isLiteMode) {
            beat = 0f
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { now ->
                val seconds = now / 1_000_000_000.0
                beat = ((seconds % 6.0) * (2.0 * kotlin.math.PI / 6.0)).toFloat()
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { sizePx = it }
            .height(DrawerStarMapHeight)
            // v477 — the map may never paint outside its own box. Its stars sit
            // close to the edge and its gradients are wide, so an unclipped draw
            // could wash the brain panel above it; clipping is free and makes the
            // panel untouchable. (v422's "no clip" was about the PLATE's visual
            // edge, which is still gone — this is a bounds clip, not a visual
            // one.)
            .clipToBounds()
            // v422 — no clip and no fill: the pattern is the whole thing, drawn
            // on the drawer's page. (The touch target still spans the full box.)
            .pointerInput(lanes, selected, starPoints) {
                detectTapGestures { tap ->
                    // The finger is the target: a 30dp halo around a star. The
                    // points measured here are the SAME ones the canvas draws
                    // (pixel space, not unit space), so a tap can never land a
                    // few dp off the star it looks like it hit.
                    if (starPoints.isEmpty()) return@detectTapGestures
                    val reach = 30.dp.toPx()
                    val hit = starPoints.indices
                        .filter { i -> (starPoints[i] - tap).getDistance() <= reach }
                        .minByOrNull { i -> (starPoints[i] - tap).getDistance() }
                    if (hit != null) {
                        onSelect(if (lanes[hit].id == selected) null else lanes[hit].id)
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // v444 — the arrival itself: a breath of scale under the fade, so
                // the sky settles into place as it comes in and gives the same
                // motion back as it leaves. Drawn in the layer, so the map is never
                // re-laid-out for it.
                .graphicsLayer {
                    val grown = reveal.value
                    alpha = grown
                    val breathing = 0.965f + 0.035f * grown
                    scaleX = breathing
                    scaleY = breathing
                }
        ) {
            val hub = Offset(size.width / 2f, size.height / 2f)
            fun at(index: Int) = starPoint(slots[index], hub, size.width, size.height)
            // ── v427 — WHAT A LIT STAR'S GLOW REACHES, in pixels. The painter
            //    and the joins below ask the SAME function, which is what lets a
            //    hairline stop exactly at the edge of the halo it runs into
            //    instead of disappearing under it. ──
            fun bornOf(index: Int): Float {
                // ── v476 — ONE BRANCH AT A TIME, DRAWN STAR TO STAR ────────
                // Every star of a family shares its family's own window, so a
                // branch comes on AS A BRANCH — its stars and its own lines
                // together — and the next family starts a beat later (the
                // member: *"the animation make it light by branch by branch"*).
                // Within its window the branch is DRAWN, not switched on: the
                // family's first lane opens it and its last closes it
                // ([BranchStarSpread]), and each branch's own progress is eased
                // ([BranchEasePower]) so it blooms rather than ramping (the
                // member: *"tune the branch animations branch by branch"*). The
                // windows still overlap a little, so the sky reads as growing
                // branches rather than as a metronome.
                val f = slotFamily.getOrElse(index) { 0 }
                val step = 1f / familyOrder.size.coerceAtLeast(1)
                val start = f * step * BranchStagger
                val span = (step * BranchSpan).coerceAtLeast(0.0001f)
                val rank = slotBranchRank.getOrElse(index) { 0f }
                val lead = start + span * BranchStarSpread * rank
                val drawSpan = (span * (1f - BranchStarSpread)).coerceAtLeast(0.0001f)
                val branch = ((lit.value - lead) / drawSpan).coerceIn(0f, 1f)
                val eased = 1f - (1f - branch).pow(BranchEasePower)
                // v444 — and the whole sky is dressed by the drawer's own
                // progress, so a close takes every star with it.
                return (eased * reveal.value).coerceIn(0f, 1f)
            }
            fun corePxOf(index: Int): Float {
                val born = bornOf(index)
                if (born <= 0f) return 0f
                val lane = lanes[index]
                val picked = lane.id == selected
                // v444 — AND THE PICKED STAR BARELY GROWS (the member: *"dont grow
                // the dot too much"*): the aura around it is what answers the tap
                // now (see the bloom below), so the disc itself moves 18% rather
                // than half again, and an untouched lane's point moves the same
                // way instead of swelling past its explored neighbours.
                // v477 — and the size a star is born at is the one size every
                // dot has (see [StarCoreDp]): a lane's knowledge never moves it.
                val core = StarCoreDp * born * (if (picked) 1.18f else 1f)
                return core.dp.toPx()
            }
            // The star the member picked, as an index — the joins that touch it
            // come on with it.
            val pickedIndex = lanes.indexOfFirst { it.id == selected }
            // A join longer than this crosses the sky rather than joining two
            // stars, so it is not drawn at all (v427). The cap follows the AVERAGE
            // of the two axes: the field is taller than it is wide now, so a
            // width-only cap would quietly drop the vertical constellations.
            val joinReach = (size.width + size.height) * 0.15f
            // ── v476 — A LINE'S SHADE, DEEPER IN LIGHT MODE ─────────────────
            // The member: *"the lines and the dots make it more noticable, in
            // light mode darker shade and similar"*. Both modes mix the hue in
            // harder than before, and the LIGHT page gets one extra step: the
            // mixed line is pulled a little toward the ink, because a coloured
            // mix on a near-white page is a pastel by construction and a pastel
            // is exactly what the member could not see. The darken follows the
            // amount, so a line that is only just being drawn out (see
            // [linkBorn]) is never a grey ghost ahead of its branch.
            fun lineMix(hue: Color, amount: Float): Color {
                val a = (amount * (if (dark) DarkLineBoost else LightLineBoost)).coerceIn(0f, 1f)
                val mixed = lerp(page, hue, a)
                return if (dark) mixed else lerp(mixed, ink, LightLineDarken * a)
            }
            // ── v457 — THE GLOW THE SKY STANDS IN (see [centreGlow]) — one
            //    radial gradient, drawn first, so every star sits on a lit page
            //    rather than on a flat one. Opaque stops that end in the page's
            //    own colour: no rim, no box, nothing to see but light.
            centreGlow?.let { glow ->
                drawRect(brush = glow, size = size)
            }
            // ── THE SKY, NOT A DIAL (v419) — the astrolabe grid is GONE.
            //    The rings and the twelve spokes were perfectly regular, and
            //    that regularity is exactly what read as too symmetric (member:
            //    "the drawer graph is bad … its too symmetric"). The stars are
            //    scattered now and a faint dust field gives the panel its depth
            //    instead. Every colour is an OPAQUE mix of the panel toward the
            //    muted ink — no alpha anywhere.
            dust.forEach { dot ->
                drawCircle(
                    color = lerp(page, muted, 0.08f + 0.12f * dot.y),
                    radius = 0.9.dp.toPx(),
                    center = Offset(dot.x * size.width, dot.y * size.height)
                )
            }
            // ── THE CONSTELLATIONS (v427). Each star is still joined to its
            //    NEAREST neighbour — never rings, never a mesh — but the JOIN
            //    itself is drawn properly now, because the flat grey hairline
            //    was the part the member called out ("the lines are not visually
            //    greate"):
            //
            //     · it wears the TWO lanes' own colours, faintly, instead of
            //       one grey — a constellation is made of the stars it joins,
            //       and grey over a coloured sky read as wire;
            //     · it STOPS AT THE GLOW: each end is pulled back to the star's
            //       own lit size, so no line pierces a halo;
            //     · a wide faint pass sits under a crisp one — the same
            //       OPAQUE-mix trick the halos use, which is the only kind of
            //       soft edge this surface allows (no alpha anywhere);
            //     · and a hop that is still long enough to cross a quarter of
            //       the sky is dropped rather than drawn: with no orbits left,
            //       a lone star's nearest neighbour can be halfway across the
            //       map, and one such line ruins the chart.
            links.forEach { link ->
                // ── v476 — A LINK LIGHTS WITH ITS BRANCH ─────────────────────
                // A hairline belongs to the family that owns it, so it comes on
                // with that branch (the later of its two stars) and draws itself
                // out as the branch does — the mesh arrives branch by branch,
                // exactly as the stars do. The ONE cross-family link per family
                // waits for both its branches, and is exempt from the join cap:
                // it is the tie that keeps the sky one web, so it is meant to
                // reach across the map.
                val linkBorn = minOf(bornOf(link.first), bornOf(link.second))
                if (linkBorn <= 0.001f) return@forEach
                val start = at(link.first)
                val end = at(link.second)
                val span = (end - start).getDistance()
                if (span > joinReach && !link.cross) return@forEach
                val trimStart = corePxOf(link.first) * HaloTrimFactor + 2.dp.toPx()
                val trimEnd = corePxOf(link.second) * HaloTrimFactor + 2.dp.toPx()
                val floor = 6.dp.toPx()
                if (span <= trimStart + trimEnd + floor) return@forEach
                val dir = (end - start) / span
                val from = start + dir * trimStart
                val to = end - dir * trimEnd
                val hue = lerp(
                    lanes[link.first].accent,
                    lanes[link.second].accent,
                    0.5f
                )
                val joined = pickedIndex == link.first || pickedIndex == link.second
                // ── v428b — THE JOIN LEANS (member: "in drawer make the pattern
                //    straight lines a little curvy just a little"). A
                //    constellation is drawn by hand and a hand bows, so each
                //    hairline is a SHALLOW arc rather than a segment: the control
                //    point is pushed off the midpoint's perpendicular by a few
                //    percent of the span, capped in dp — a join across the sky is
                //    barely bowed, and a short one is not a squiggle. The side is
                //    fixed by the pair's own indices, so the drawing is identical
                //    on every recomposition and through the whole light-up. ──
                val bow = minOf(span * 0.10f, 6.dp.toPx())
                val side = if ((link.first + link.second) % 2 == 0) 1f else -1f
                // v444 — THE MESH DRAWS ITSELF: each hairline runs out from its own
                // star toward the one it joins as the drawer comes in, which is
                // what makes the map arrive as a constellation being drawn rather
                // than as a finished chart being shown (see [reveal]).
                val tip = from + (to - from) * linkBorn
                val control = (from + tip) / 2f + Offset(-dir.y, dir.x) * (bow * side)
                val hair = Path().apply {
                    moveTo(from.x, from.y)
                    quadraticBezierTo(control.x, control.y, tip.x, tip.y)
                }
                // v457 — a notch stronger than v444's pair: the member asked for
                // the pattern's shade to be more noticeable, and the hairlines are
                // what draws the mesh the eye follows.
                drawPath(
                    path = hair,
                    color = lineMix(hue, (if (joined) 0.30f else 0.20f) * linkBorn),
                    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = hair,
                    color = lineMix(hue, (if (joined) 0.62f else 0.46f) * linkBorn),
                    style = Stroke(width = 1.15.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // ── The stars. ──
            //
            // v422 — THE PICK MAKES THE STAR COME ON. It grows and its aura
            // brightens toward the lane's own accent; there is no ring any more,
            // because a circle drawn around a star read as furniture rather than
            // as the star answering.
            //
            // v457 — AND EACH STAR IS ONE DRAW OP. The aura is the cached gradient
            // in [starHalos] (a falloff, not rings — see its note), the twinkle and
            // the birth are one translate+scale over it, and an unexplored lane is
            // still the solid dim point v422 asked for.
            lanes.forEachIndexed { index, _ ->
                // The staggered light-up: each star waits its own turn, so the sky
                // fills in as a sweep rather than blinking on.
                val born = bornOf(index)
                if (born <= 0f) return@forEachIndexed
                val settled = starPoints.getOrNull(index) ?: return@forEachIndexed
                val halo = starHalos.getOrNull(index)
                if (halo == null) {
                    // v476 — an untouched lane is still the solid dim point v422
                    // settled on, only DEEPER now (the member asked for the dots
                    // to be more noticeable, darker in light mode).
                    val dim = lerp(page, muted, (0.34f + 0.40f * born).coerceIn(0f, 1f))
                    drawCircle(
                        color = if (dark) dim else lerp(dim, ink, 0.16f * born),
                        // v477 — the same one size every other dot is drawn at.
                        radius = corePxOf(index),
                        center = settled
                    )
                    return@forEachIndexed
                }
                val (corePx, brush) = halo
                // ── v457 — THE STAR COMES OUT OF THE HUB ────────────────
                //
                // A newborn star sits a tenth of its own distance closer to the
                // middle and slides to its place while it grows, so the sky reads
                // as a constellation being DRAWN outward from its centre rather
                // than switched on star by star. It reverses on the way out for
                // free: every star is driven by the same [born] the close is.
                val pull = 1f - 0.10f * (1f - born)
                val centre = Offset(
                    hub.x + (settled.x - hub.x) * pull,
                    hub.y + (settled.y - hub.y) * pull
                )
                // The twinkle moves the RADIUS only — a star breathes, it does not
                // change colour.
                val flick = 1f + 0.07f * sin(beat + index * 1.9f)
                val sizeNow = born * flick
                withTransform({
                    translate(centre.x - settled.x, centre.y - settled.y)
                    scale(sizeNow, sizeNow, pivot = settled)
                }) {
                    drawCircle(brush = brush, radius = corePx * HALO_REACH, center = settled)
                }
            }
        }
    }
}

/**
 * How many faint dust specks fill the sky behind the stars.
 *
 * v422 — 56, not 46: the sky is taller now, and the dust is what gives a
 * backgroundless scatter its depth, so it has to cover the area it grew into.
 */
private const val STAR_DUST_COUNT = 56

/**
 * v414 — ONE LANE'S PLACE ON THE CHART: a polar SLOT (angle + radius), not a
 * point. Keeping the slot (rather than a finished offset) is what lets the same
 * lane land correctly in a box of any shape.
 */
private data class StarSlot(val angle: Float, val radius: Float)

/**
 * Where [slot] sits, in pixels, around [hub] inside a [width]×[height] sky.
 * Shared by the painter and the hit test, so a tap can never miss the star it
 * looks like it hit.
 *
 * v427 — THE FIELD FOLLOWS THE BOX. It used to multiply the radius by the
 * SHORTER side on BOTH axes, which kept the scatter perfectly round but also
 * meant a taller sky could not hold a single extra star's worth of reach: the
 * drawer is a fixed 336dp wide (its map 304dp across), so the shorter side was
 * the WIDTH and the height above it went to waste. Each axis is used now — the
 * sky reads longer, and the points themselves are drawn in dp (see the bloom in
 * [DrawerLaneStarMap]), so a lit star is still a perfect circle.
 */
private fun starPoint(slot: StarSlot, hub: Offset, width: Float, height: Float): Offset =
    Offset(
        x = hub.x + cos(slot.angle) * (width * 0.5f) * slot.radius,
        y = hub.y + sin(slot.angle) * (height * 0.5f) * slot.radius
    )

/**
 * How far past a star's own core its glow is worth stopping a line at: the
 * painter's mid ring sits at 1.35 of the core (see the bloom), and a line that
 * starts there meets the halo's edge instead of vanishing under it.
 */
private const val HaloTrimFactor = 1.35f

/**
 * How far a lit star's glow reaches, as a multiple of its core (v457).
 *
 * The gradient's radius, the circle that bounds it and the halo the joins stop at
 * all read from this one number, so the three can never drift apart.
 */
private const val HALO_REACH = 2.7f

// ── v476 — THE CONSTELLATION'S OWN NUMBERS ────────────────────────────────
//
// The member: *"kee the pattern same same dot connections group like similar
// category in that branch and the animation make it light by branch by branch,
// the lines and the dots make it more noticable, in light mode darker shade and
// similar"*. These numbers do that and nothing else (see [bornOf], [lineMix],
// [starScatterByFamily], [StarCoreDp] and [FamilyPull]).

/**
 * Where a family's window of the light-up begins, as a share of one family's
 * step through the sweep. Under 1 so a branch starts before the previous one has
 * entirely finished (the member asked for branches, not a metronome) while still
 * leaving the last branch room to complete before the sweep ends.
 */
private const val BranchStagger = 0.90f

/**
 * How long one family's own window runs, as a multiple of its step. Over 1 so the
 * windows OVERLAP a little — the sky grows as branches, not as blinking groups.
 */
private const val BranchSpan = 1.28f

/**
 * How far a lane's own accent is pulled toward the ink when it becomes a halo in
 * LIGHT mode (v476). A lit star's bloom is a wash of its accent on a near-white
 * page, and a pure accent there is a pastel the member could not read ("in light
 * mode darker shade"); dark mode keeps the true accent.
 */
private const val LightDotDarken = 0.18f

/**
 * How much harder the hairlines mix their hue, per theme (v476). More than 1 so
 * both modes are more noticeable than before (the member: "the lines … make it
 * more noticable"), and the light page asks for a little more boost because a
 * coloured mix loses contrast against white.
 */
private const val DarkLineBoost = 1.15f
private const val LightLineBoost = 1.30f

/**
 * The one extra step the LIGHT page's hairlines take: after mixing the hue in,
 * the line is pulled this far toward the ink, because a coloured mix on a
 * near-white page reads as a pastel and a pastel is exactly what the member
 * could not see. It follows the line's own amount (see [lineMix]), so a line that
 * is only just being drawn out is never a grey ghost ahead of its branch.
 */
private const val LightLineDarken = 0.30f

/**
 * v477 — HOW BIG A DOT IS, and how far a lane is pulled toward its family.
 *
 * Windows of the two v477 numbers, both answered by the member's own report on
 * v476: the dots had been growing with knowledge and the scatter had been rebuilt
 * as tight family clusters. Every star's core is [StarCoreDp]; the lane's
 * knowledge decides its colour and whether it carries a lit halo, never how fat it
 * is (see [starHalos] and [corePxOf]). Every lane lands on the ORIGINAL Vogel
 * spiral and then moves [FamilyPull] of the way toward its family's centre of mass
 * (see [starScatterByFamily]) — grouping you can see, pattern you can still read.
 */
private const val StarCoreDp = 3.0f
private const val FamilyPull = 0.35f

/**
 * v476 — HOW A BRANCH DRAWS ITSELF.
 *
 * The member: *"the animation make it light by branch by branch"* and *"tune the
 * branch animations branch by branch"*. A family's stars were all lit at once, which
 * made a branch pop as a block; a share of the branch's window now spreads them
 * ([BranchStarSpread]) so the constellation is DRAWN star to star inside its own
 * turn, and each branch's own progress is eased ([BranchEasePower]) so it blooms
 * rather than ramping linearly.
 */
private const val BranchStarSpread = 0.55f
private const val BranchEasePower = 1.8f

/**
 * v419 — LANES ON A GOLDEN-ANGLE SCATTER (this replaced the v414 lattice).
 *
 * The member: "the drawer graph is bad … the previous version was at least
 * better … its too symmetric". The lattice was legible but rigid — perfect
 * rings, evenly spaced stars, twelve spokes — and a sky is not a machine dial.
 * This is the phyllotaxis idea the member preferred, kept WELL-SPREAD (the
 * reason the lattice was tried at all) and given a small deterministic wobble
 * so no two neighbours sit at the same radius or the same angle.
 *
 *  * the i-th star sits at the GOLDEN ANGLE times i, the classic Vogel spiral,
 *    so the disc fills evenly at any lane count instead of clumping;
 *  * its radius grows with sqrt(i / count), so the sky reaches the panel's
 *    edge without crowding the hub;
 *  * a small HASHED jitter (never random — the same lanes always land in the
 *    same places, so the map stays the landmark the member learns) breaks the
 *    symmetry;
 *  * knowledge never moves a star; it changes colour and light only.
 *
 * v477 — THE SAME SHAPE, GENTLY GROUPED BY FAMILY. v476 rebuilt the scatter as
 * one TIGHT cluster per family, and the member read that as a different pattern
 * entirely: *"the scattering is bad i asked not to chnage the attern too much …
 * very small scatters"*. So nothing is rebuilt. Every lane lands exactly where the
 * original per-lane Vogel spiral put it (the same formula, untouched), and is then
 * pulled only a little ([FamilyPull]) toward its own family's centre of mass — the
 * sky the member knows is still the sky, with similar lanes loosened into soft
 * neighbourhoods rather than herded into knots.
 */
private fun starScatterByFamily(familyOf: List<CategoryFamily>): List<StarSlot> {
    if (familyOf.isEmpty()) return emptyList()
    // The golden angle in radians — Vogel's constant, irrational on purpose so
    // successive points never fall into a repeating spoke.
    val goldenAngle = 2.3999632f
    val count = familyOf.size
    // ── 1) THE ORIGINAL SPIRAL, LANE BY LANE (the v419 formula, unchanged) ──
    // Unit space (the same ellipse [starPoint] scales), so every lane lands where
    // it always did and the pattern is the one the member learns.
    val start = List(count) { i ->
        val wobble = (((i * 2654435761L) and 7L)).toInt() - 3
        val base = kotlin.math.sqrt((i + 0.55f) / count)
        val radius = (0.20f + 0.74f * base + wobble * 0.011f).coerceIn(0.16f, 0.95f)
        val angle = i * goldenAngle + wobble * 0.055f
        Offset(cos(angle) * radius, sin(angle) * radius)
    }
    // ── 2) EACH FAMILY'S CENTRE OF MASS ───────────────────────────────────
    val sums = HashMap<CategoryFamily, Offset>()
    val members = HashMap<CategoryFamily, Int>()
    start.forEachIndexed { i, point ->
        val family = familyOf[i]
        sums[family] = (sums[family] ?: Offset.Zero) + point
        members[family] = (members[family] ?: 0) + 1
    }
    val centres = sums.mapValues { (family, sum) ->
        val n = members[family] ?: 1
        Offset(sum.x / n, sum.y / n)
    }
    // ── 3) AND A GENTLE PULL TOWARD IT ────────────────────────────────────
    // A fraction of the way in and no further ([FamilyPull]): the member asked for
    // the same pattern with similar lanes reading together, not for the tight
    // clusters v476 drew.
    return start.mapIndexed { i, point ->
        val centre = centres[familyOf[i]] ?: point
        val pulled = point + (centre - point) * FamilyPull
        val clamped = Offset(
            pulled.x.coerceIn(-0.95f, 0.95f),
            pulled.y.coerceIn(-0.95f, 0.95f)
        )
        val radius = clamped.getDistance().coerceIn(0.16f, 0.95f)
        val angle = if (radius <= 0.0001f) 0f else kotlin.math.atan2(clamped.y, clamped.x)
        StarSlot(angle = angle, radius = radius)
    }
}

/**
 * v476 — ONE HAIRLINE ON THE SKY, and whether it is the tie between two families.
 *
 * The old nearest-neighbour pass answered a `Pair<Int, Int>`; the family map has
 * to KNOW which links cross families, because those are the ones exempt from the
 * join cap (they are meant to reach) and the ones that wait for BOTH branches to
 * light (see the painter).
 */
private data class StarLink(val first: Int, val second: Int, val cross: Boolean)

/**
 * v476 — THE CONSTELLATION'S HAIRLINES, GROUPED BY FAMILY.
 *
 * Two rules, straight from the member's ask (*"kee the pattern same same dot
 * connections group like similar category in that branch"* and, of the whole sky,
 * *"one connected sky"*):
 *
 *  * **Within a family** each star joins its NEAREST FAMILY-MATE — the old
 *    nearest-neighbour rule, scoped to the branch — so a family reads as its own
 *    small constellation rather than a knot shared with strangers;
 *  * **Between families** each family keeps exactly ONE tie, to the nearest star
 *    of any OTHER family. That single long hairline is what keeps the sky one web
 *    instead of twelve floating islands, and it is marked `cross` so the painter
 *    can let it reach and light it only once BOTH branches are on.
 *
 * Measured in unit space (the same ellipse [starPoint] scales) and computed once
 * per lane layout.
 */
private fun starLinksGrouped(
    slots: List<StarSlot>,
    familyOf: List<CategoryFamily>
): List<StarLink> {
    if (slots.size < 2) return emptyList()
    val points = slots.map { slot ->
        Offset(cos(slot.angle) * slot.radius, sin(slot.angle) * slot.radius)
    }
    val links = LinkedHashSet<StarLink>()
    // Within a family: the one mate a star is actually closest to.
    slots.indices.forEach { i ->
        val mate = slots.indices
            .filter { it != i && familyOf.getOrNull(it) == familyOf.getOrNull(i) }
            .minByOrNull { d -> (points[d] - points[i]).getDistanceSquared() }
        if (mate != null) {
            links.add(StarLink(minOf(i, mate), maxOf(i, mate), cross = false))
        }
    }
    // Between families: ONE tie per family, to the nearest star outside it.
    familyOf.distinct().forEach { family ->
        val mine = slots.indices.filter { familyOf.getOrNull(it) == family }
        val others = slots.indices.filter { familyOf.getOrNull(it) != family }
        if (mine.isEmpty() || others.isEmpty()) return@forEach
        var bestFirst = -1
        var bestSecond = -1
        var bestDist = Float.MAX_VALUE
        mine.forEach { i ->
            others.forEach { j ->
                val d = (points[j] - points[i]).getDistanceSquared()
                if (d < bestDist) {
                    bestDist = d
                    bestFirst = i
                    bestSecond = j
                }
            }
        }
        if (bestFirst >= 0) {
            links.add(
                StarLink(
                    minOf(bestFirst, bestSecond),
                    maxOf(bestFirst, bestSecond),
                    cross = true
                )
            )
        }
    }
    return links.toList()
}

/**
 * v419 — A FAINT DUST FIELD, so the panel is a SKY rather than a flat plate.
 *
 * A cheap deterministic pseudo-random (a linear congruential step) scatters
 * tiny dim points in UNIT space; the painter scales them by the panel, so the
 * dust never stretches. Not random: the same specks land in the same places on
 * every open.
 */
private fun starDust(count: Int): List<Offset> {
    val out = ArrayList<Offset>(count)
    var seed = 0x2545F491
    repeat(count) {
        seed = seed * 1103515245 + 12345
        val x = ((seed ushr 8) and 0xFFFF) / 65535f
        seed = seed * 1103515245 + 12345
        val y = ((seed ushr 8) and 0xFFFF) / 65535f
        out.add(Offset(x, y))
    }
    return out
}

/** One pane of the drawer's brain strip: accent glyph, big value, quiet
 *  label — the same construction the Stats page's progress card uses. */
@Composable
private fun DrawerBrainStat(
    glyph: String,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CurioIcon(glyph, null, tint = tint, size = 15.dp)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** v174 — the drawer hero's celestial palette: a dreamy pre-dawn sky. Light
 *  mode is a pale seafoam gradient with deep seafoam ink; dark mode is a
 *  deep twilight teal with warm cream ink ("looking at a peaceful sky just
 *  before sunrise"). Returns (skyTop, skyBottom, readableInk). *//**
 * v460 — THE ONE SHAPE A GLOW ON THIS SURFACE MAY HAVE.
 *
 * Every light in the drawer's sky (the wash the whole map stands in, and each
 * star's own aura) is a radial gradient from a tint back to the page, and the
 * member's report of the old one was *"i can see the edges in dark mode"* — the
 * rings a four-stop gradient shows on a dark fill, because a piecewise-linear
 * ramp changes slope at every stop and the eye draws a line where it does.
 *
 * So the stops are SAMPLED off an eased curve rather than listed by hand: at
 * [GlowSteps] fine steps, with `(1 - t) ^ power`. Two properties come out of
 * that, and both are why it reads as light instead of as rings:
 *
 *  · no corner anywhere in the ramp, so there is no slope change for the eye to
 *    find; and
 *  · `power > 1` makes the falloff flat at the rim (`mix → 0` with zero slope),
 *    so the edge of the gradient's own radius is not an edge at all.
 *
 * The list is built once per brush (the brushes are cached in COMPOSITION) and
 * costs nothing per frame, which is why the step count can be generous.
 */
private fun glowStops(
    page: Color,
    tint: Color,
    strength: Float,
    power: Float = 2f,
    steps: Int = GlowSteps
): List<Color> {
    // A brush needs at least two stops, so a degenerate step count still answers
    // with a ramp rather than an empty list.
    if (steps <= 1) return listOf(page, page)
    return List(steps) { index ->
        val t = index.toFloat() / (steps - 1)
        val mix = strength * (1f - t).pow(power)
        lerp(page, tint, mix.coerceIn(0f, 1f))
    }
}

/** How much of the brand accent the sky's centre wash is mixed from. */
private const val GlowTintMix = 0.22f

/**
 * The wash's own strength, per theme.
 *
 * Not one number, and not the same direction as a shadow: a wash on a dark page
 * needs LESS of it to read (the old 0.66 was already strong enough to show its
 * own steps), and the same 0.66 on a near-white page is invisible — which is the
 * "in light mode its not visible" half of the member's report.
 */
private const val DarkGlowStrength = 0.62f
private const val LightGlowStrength = 0.92f

/** How far the centre wash reaches, as a share of the map's longer side. */
private const val GlowReach = 0.72f

/** How finely an eased glow brush is sampled (see [glowStops]). */
private const val GlowSteps = 18

// ── v461 — THE ANNOTATION BELONGS TO THIS FUNCTION AND MUST STAY HERE ──────
//
// v460's own edit put its helper's doc ABOVE this line, which pushed the
// `@Composable` that had been sitting here down onto `glowStops` — the v458 trap
// repeating verbatim: **`@Composable` binds to the next DECLARATION, and a KDoc
// block in between is not a barrier**. The build said so plainly ("Functions
// which invoke @Composable functions must be marked with the @Composable
// annotation" at this function, plus two "@Composable invocations can only happen
// from the context of a @Composable function" where `glowStops` is called from
// `remember { }`). Read this line before inserting anything above it.
@Composable
private fun drawerSkyColors(): Triple<Color, Color, Color> {
    return if (isCurioDarkTheme()) {
        Triple(Color(0xFF12313A), Color(0xFF1D4750), Color(0xFFF4F1E7))
    } else {
        Triple(Color(0xFFC2E8DE), Color(0xFFE9F6F0), Color(0xFF2C5A53))
    }
}

/** v176 — the drawer's end-of-drawer footer: the user's cropped planet SVG
 *  sits FLAT at the very bottom of the drawer (no box, no shadow, no
 *  scaffolding) and fades into the surface at its bottom edge so the art
 *  doesn't look like it's floating. The version + "Made with curiosity"
 *  line sits inside that fade, at the end of the footer. The SVG loads
 *  through Coil's SvgDecoder. */
// v203 — the footer's height is shared with the list's bottom padding so
// the pinned footer never covers the last row.
private val DrawerFooterHeight = 150.dp

@Composable
private fun DrawerFooter() {
    val context = LocalContext.current
    val model = remember(context) {
        ImageRequest.Builder(context)
            .data(R.raw.drawer_footer)
            .decoderFactory(SvgDecoder.Factory())
            .crossfade(true)
            .build()
    }
    val surfaceColor = MaterialTheme.colorScheme.surface
    // v203 — theme-aware credits ink: the fixed khaki (#7E6E50) vanished on
    // the near-black surface in dark mode (user: "the v1.10 made with
    // curiocity text sint visible"). Dark mode now uses a warm parchment
    // light so the credits read over the fade in both themes.
    val footerInk = if (isCurioDarkTheme()) Color(0xFFC9BC9D) else Color(0xFF7E6E50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DrawerFooterHeight)
            .background(surfaceColor)
            .navigationBarsPadding()
    ) {
        // The art, bottom-anchored and filling the drawer's full width —
        // cropped from the top so the planet reads at the bottom end.
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        )
        // Bottom fade — the illustration melts into the drawer surface so
        // it doesn't read as a floating panel at the end of the footer.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to surfaceColor
                    )
                )
        )
        // Version + credits, sitting in the fade at the end of the footer.
        // (v208f — the footer Box now carries the navigationBarsPadding, so
        // the credits no longer need their own inset.)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        ) {
            Text(
                "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = footerInk
            )
            Text("·", style = MaterialTheme.typography.labelSmall, color = footerInk.copy(alpha = 0.6f))
            Text(
                "Made with curiosity",
                style = MaterialTheme.typography.labelSmall,
                color = footerInk
            )
            CurioIcon("favorite", null, tint = footerInk.copy(alpha = 0.8f), size = 12.dp)
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════
// Greeting helpers
// ═══════════════════════════════════════════════════════════════════════

/**
 * v411 — HOME'S GREETING IS ONE FIXED LINE NOW.
 *
 * It used to be a time-of-day word (Good morning / afternoon / evening, and
 * "Welcome back" after hours). The member named the copy they want — "make the
 * home screen welcome back curiours explorer" — so the greeting no longer
 * depends on the clock: Home always says the same warm line, and the NAME under
 * it (which defaults to "Curious Explorer") completes the sentence. The hero
 * and the glass header read the same string, so the two header styles can
 * never greet differently.
 */
private fun homeGreeting(): String = "Welcome back"

// ═══════════════════════════════════════════════════════════════════════
// Explore-session topic row (recently explored / recently unexplored)
// ══════════════════════════════════════════════��════════════════════════

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ExploreTopicRow(
    category: CurioCategory,
    topicName: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String? = null
) {
    // v455 — no `hold`: Home's recents are tap-only (see the note at the
    // recents preview). The hold menu lives on the Recents page's own rows.
    val accent = category.themedAccent()
    // Solid category-tinted card in light mode — the recents topics wear a
    // solid background in their category's color family (matching the
    // gradient identity), instead of a backgroundless row. v115 — dark
    // mode: the Home recents go back to plain dark surface cards (no
    // category tint on pitch black).
    val rowShape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // v98 — dark pill: previous color + pill shape kept; the white
            // catch stays at the TOP EDGE only (curioGlassEdge) — the
            // full-pill inner glow is gone.
            .curioGlassEdge(rowShape)
            .combinedClickable(onClick = onClick),
        shape = rowShape,
        color = if (isCurioDarkTheme()) MaterialTheme.colorScheme.surfaceContainerLow else category.categorySurface(),
        // v27u — recents rows sit on a soft 2dp lift.
        shadowElevation = 2.dp
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
                        // v198 — the pill wears a SHADED category chip: the
                        // accent pulled toward the card surface — ~30% in
                        // light (a solid shaded chip on the tinted card,
                        // replacing the old 14% blend that vanished) and
                        // ~38% in dark (visibly tinted on the dark card,
                        // never the old near-invisible blend). Pastel light
                        // uses the deep same-hue ink as the shade so the
                        // airy pastel twin doesn't wash the pill away.
                        val tagShade = if (AppPreferences.pastelColorsState && !isCurioDarkTheme())
                            category.categoryInk() else accent
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = lerp(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                tagShade,
                                if (isCurioDarkTheme()) 0.38f else 0.30f
                            ),
                            // Same hairline rim as the detail page's #tag
                            // chips — the deep ink text + pastel fill alone
                            // read muddy on the tinted card (v7.32).
                            // v27u — pill lift trimmed to 1dp so it reads as
                            // a chip on the card rather than a floating tile.
                            shadowElevation = 1.dp
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
                // v476 — a blank subtitle draws NOTHING (it used to render an
                // empty Text, which reserved its line). The row's state is then
                // carried by the chip alone (see the unexplored call site).
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            CurioForwardArrow(
                // A blank subtitle leaves the arrow with no label to read, so it
                // falls back to the topic's own name.
                contentDescription = subtitle.ifEmpty { topicName },
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
            // v28 — dark mode elevation visibility (glow + hairline).
            .curioDarkGlow(2.dp, RoundedCornerShape(24.dp))
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
            // ─��� End session ��� the card's top corner ─────────────────
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
                    // v28 — dark mode elevation visibility.
                    .curioDarkGlow(2.dp, CircleShape)
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
                        // v226 — "Express yourself": the reveal page's
                        // write-it-down language, on every surface.
                        Text("Express yourself", style = MaterialTheme.typography.labelLarge)
                    }
                    // v226 — "Keep exploring" is a proper pill now: the
                    // old borderless OutlinedButton read as a floating
                    // label. Soft accent-tinted fill + play glyph — the
                    // same language as the card's corner Stop button.
                    Surface(
                        onClick = onKeepExploring,
                        shape = RoundedCornerShape(50),
                        color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.14f),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .curioDarkGlow(2.dp, RoundedCornerShape(50))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
                        ) {
                            CurioIcon(CurioIcons.PlayArrow, null, tint = exploreInk, size = 16.dp)
                            Text("Keep exploring", style = MaterialTheme.typography.labelLarge, color = exploreInk)
                        }
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
                // v474 — the row is tappable and says so by being a row; the
                // state is the information.
                "Paused at ${formatElapsed(session.elapsedMillis())}",
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

/**
 * v226 — the last CANCELLED explore session, offered back as a resumable
 * row (same language as [QueuedExploreRow]): replay glyph, topic, banked
 * elapsed readout, and a discard ✕. Tapping resumes the session with its
 * banked time intact.
 */
@Composable
private fun CancelledExploreRow(
    session: ExploreSession,
    onResume: () -> Unit,
    onDiscard: () -> Unit
) {
    val ink = CurioCategories.byId(session.categoryId).categoryInk()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onResume)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CurioIcon(CurioIcons.Replay, null, tint = ink, size = 22.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.topicName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Cancelled at ${formatElapsed(session.elapsedMillis())}",
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
                CurioIcons.Close, "Discard cancelled explore",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 16.dp,
                modifier = Modifier.padding(5.dp)
            )
        }
    }
}
