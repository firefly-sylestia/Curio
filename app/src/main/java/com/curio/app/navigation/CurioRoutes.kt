package com.curio.app.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController

/**
 * Centralized route names for the Curio NavHost — see Curio navigation contract.
 *
 * Routes that take arguments use placeholder-path syntax that maps directly
 * to Compose Navigation `composable("route/{argName}")` patterns. The
 * bottom-nav tab routes are flat (no nested graph) because the placeholder
 * phase keeps everything in one NavHost — switching tabs uses saveState /
 * restoreState so back-stack inside each tab is preserved.
 */
/**
 * Out-of-band handoff for the Lightbox target URI.
 *
 * The image URI is passed here (not through the nav route string) because
 * Compose Navigation auto-decodes path arguments — combined with a second
 * decode in the NavHost this corrupts percent-encoded content URIs and the
 * image never loads. Setting [uri] right before navigating and reading it in
 * the Lightbox keeps the URI byte-for-byte intact.
 */
object LightboxTarget {
    var uri: String? = null
}

/**
 * v208e — out-of-band Z-INDEX slot for the Topic Reveal's Like/Dislike pill.
 *
 * The floating nav bar is composed ABOVE the NavHost destinations, so the
 * pill (deep inside the reveal destination) would slide in BEHIND the
 * collapsing nav pill. The reveal registers its pill composable here and
 * the NavHost composes the slot in its own overlay layer AFTER the bar — so
 * the Like/Dislike renders ON TOP of the collapsing nav pill during the
 * handoff (user: "place the like and dislike pill z index above the home
 * nav pill… keep it overlap"). The slot clears when the reveal route
 * leaves (the reveal's DisposableEffect) so no stale pill can linger.
 */
object SentimentPillHost {
    var content: (@Composable () -> Unit)? by mutableStateOf(null)
}

/**
 * Out-of-band handoff for opening the Cabinet pre-filtered to one lane
 * (v39 — Profile's "Your lanes" tiles).
 *
 * The Cabinet is a bottom-nav tab route, so its filter can't ride a nav
 * argument without breaking tab state restoration. Instead the caller
 * stashes the lane's [CategoryId] name here before navigating, and the
 * Cabinet screen consumes it once on first composition (a monotonic bump
 * keyed in a LaunchedEffect so the NavHost recomposes when it fires).
 */
object PendingCabinetFilter {
    private var categoryName: String? = null
    private val counter = mutableIntStateOf(0)

    /** Stashes a lane to open the Cabinet with when the tab next appears. */
    fun request(categoryId: com.curio.app.data.CategoryId) {
        categoryName = categoryId.name
        counter.intValue++
    }

    /** Monotonic bump — the Cabinet keys its consume-effect on this. */
    val trigger: Int get() = counter.intValue

    /** Consumes and returns the pending lane's category name, if any. */
    fun take(): String? {
        val name = categoryName ?: return null
        categoryName = null
        return name
    }
}

/**
 * Out-of-band handoff for the "Done exploring" notification action.
 *
 * The action's broadcast receiver tears the session down and launches
 * MainActivity with the topic's category slug + name as extras. The extras
 * are stashed here (like [LightboxTarget]) because MainActivity may be
 * cold-started (onCreate) or already running (onNewIntent), and the NavHost
 * is the only place that can navigate to the write-it-down entry page with
 * a HOME-anchored back stack. The NavHost consumes the target once it is on
 * a stable root route.
 */
object PendingEntryOpen {
    const val EXTRA_CATEGORY_SLUG = "com.curio.app.extra.OPEN_ENTRY_CATEGORY_SLUG"
    const val EXTRA_TOPIC_NAME = "com.curio.app.extra.OPEN_ENTRY_TOPIC_NAME"

    private var categorySlug: String? = null
    private var topicName: String? = null
    // Compose-observable bump: capture() may run from MainActivity (outside
    // composition), so the NavHost must recompose when it fires — a plain
    // Int would never invalidate the LaunchedEffect key.
    private val counter = mutableIntStateOf(0)

    /** Stashes a deep-link target carried by [intent], if one is present. */
    fun capture(intent: Intent?) {
        val slug = intent?.getStringExtra(EXTRA_CATEGORY_SLUG)
        val name = intent?.getStringExtra(EXTRA_TOPIC_NAME)
        if (slug != null && name != null) {
            categorySlug = slug
            topicName = name
            counter.intValue++
        }
    }

    /** Monotonic bump — the NavHost keys its open-effect on this. */
    val trigger: Int get() = counter.intValue

    /** Consumes and returns the pending target, if one is set. */
    fun take(): Pair<String, String>? {
        val slug = categorySlug ?: return null
        val name = topicName ?: return null
        categorySlug = null
        topicName = null
        return slug to name
    }
}

/**
 * Out-of-band handoff for the daily-reminder notification tap.
 *
 * The daily shuffle reminder ("A little curiosity awaits") carries a boolean
 * extra so tapping it opens the app ON the Spin deck (the shuffle page it
 * nudges toward) instead of plain Home. Like [PendingEntryOpen], the extra is
 * stashed here because MainActivity may be cold-started (onCreate) or already
 * running (onNewIntent), and the NavHost is the only place that can navigate
 * with the correct back stack. The NavHost consumes the request once it is on
 * a stable root route.
 */
object PendingSpinOpen {
    const val EXTRA_OPEN_SPIN = "com.curio.app.extra.OPEN_SPIN"

    private var pending = false
    // Compose-observable bump: capture() may run from MainActivity (outside
    // composition), so the NavHost must recompose when it fires — a plain
    // Boolean would never invalidate the LaunchedEffect key.
    private val counter = mutableIntStateOf(0)

    /** Stashes the spin-open request carried by [intent], if one is present. */
    fun capture(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_SPIN, false) == true) {
            pending = true
            counter.intValue++
        }
    }

    /** Monotonic bump — the NavHost keys its open-effect on this. */
    val trigger: Int get() = counter.intValue

    /** Consumes and returns whether the Spin deck should be opened. */
    fun take(): Boolean {
        val p = pending
        pending = false
        return p
    }
}

object CurioRoutes {

    // ── Bottom-nav tabs (always rendered with the bottom nav bar)
    const val HOME = "home"
    const val SPIN = "spin"
    const val CABINET = "cabinet"

    // ── Splash / gates (no bottom nav)
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"

    // ── Inside the Spin flow (Reveal keeps bottom nav for morph stability)
    const val PICKER = "picker"
    const val SPIN_WITH_CATEGORY = "spin/{categorySlug}"
    // Optional ?browse=1 marks a topic opened from the Browse Topics
    // database: the reveal page renders read-only (no explore CTA, no
    // like/dislike, no recents recording, and "Already watched" confirms
    // without the write-about-it dialog).
    const val REVEAL = "reveal/{categorySlug}/{topicName}?browse={browse}"
    const val CAPTURE = "capture/{categorySlug}/{topicName}"

    // ── Push destinations (no bottom nav)
    const val PROFILE = "profile"
    const val QUESTS = "quests"
    // v174c — the Curiosity Stats page (observatory constellation + all
    // lifetime stats), reachable from the drawer and Profile.
    const val STATS = "stats"
    const val ENTRY_DETAIL = "detail/{entryId}"
    const val EDIT_MOODBOARD = "edit-moodboard/{entryId}"
    const val EDIT_ENTRY = "edit-entry/{entryId}"
    const val SETTINGS = "settings"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_PREFERENCES = "settings/preferences"
    const val SETTINGS_RECORDING = "settings/recording"
    const val SETTINGS_DATA = "settings/data"
    const val EXPERIMENTS = "experiments"
    const val USER_EXPERIMENTS = "user_experiments"
    // v264 — the liquid-glass widget test bed (wallpaper + draggable glass shapes).
    const val GLASS_WIDGET_LAB = "glass_widget_lab"
    const val GLASS_WIDGET_EDITOR = "glass_widget_editor"
    const val MANAGE_CATEGORIES = "manage-categories"
    const val TOPIC_HISTORY = "topic-history"
    const val RECENTS_ALL = "recents"
    const val LIGHTBOX = "lightbox"
    const val CRASH = "crash"
    const val BUG_REPORT = "bug-report"
    const val SUPPORT = "support"
    // v112 — dedicated Updates sub-page (own UI, replaced the Support card).
    const val UPDATES = "updates"
    const val PROMO = "promo"
    // v26 — recycle bin for soft-deleted captures (Settings entry).
    const val RECYCLE_BIN = "recycle-bin"
    const val DATABASE = "database"
    // Share hub — browse every share-card design, pick a topic, share a card.
    const val SHARE_HUB = "share-hub"
    const val FIELDMIND_OBSERVATION = "fieldmind-observation"
    // v8.34 — the Pet designer playground (custom pet look, Settings entry).
    const val PET_DESIGNER = "pet-designer"

    // ── Route builders ──────────────────────────────────────────────────────
    fun spinWithCategory(slug: String) = "spin/$slug"
    /** Multi-category launch — comma-joined slugs ("spin/artists,albums"). */
    fun spinWithCategories(slugs: List<String>) = "spin/${slugs.joinToString(",")}"
    fun revealFor(categorySlug: String, topicName: String) =
        "reveal/$categorySlug/${Uri.encode(topicName)}"
    /** Opens a topic in the read-only Browse-Topics mode (see [REVEAL]). */
    fun revealForBrowse(categorySlug: String, topicName: String) =
        "reveal/$categorySlug/${Uri.encode(topicName)}?browse=1"
    fun captureFor(categorySlug: String, topicName: String) =
        "capture/$categorySlug/${Uri.encode(topicName)}"
    fun entryDetail(entryId: String) = "detail/$entryId"
    /** Edit a saved GalleryWall (mood board) entry — preloads + re-saves in place. */
    fun editMoodBoard(entryId: String) = "edit-moodboard/$entryId"
    /**
     * Edit a saved multi-section entry — reopens every take (the whole
     * Portfolio) in the universal editor, preloaded + re-saved in place.
     */
    fun editEntry(entryId: String) = "edit-entry/$entryId"
    /** Sets the out-of-band target and returns the arg-free Lightbox route. */
    fun lightbox(imageUrl: String): String {
        LightboxTarget.uri = imageUrl
        return LIGHTBOX
    }

    /**
     * Bottom-nav tab route templates. Reveal is included as the Spin tab's
     * continuation so bottom-nav selection/back-stack logic can still treat
     * it as Spin-adjacent, but CurioNavHost hides the actual bar on Reveal
     * and reserves an equal-height torn placeholder for morph stability.
     */
    val bottomNavRoutes: Set<String> = setOf(HOME, SPIN, CABINET, REVEAL)

    /**
     * Route PREFIXES where the bottom navigation bar should be visible.
     * Use this (not [bottomNavRoutes]) when checking `destination.route`
     * — the Nav library returns the route TEMPLATE (e.g.
     * `spin/{categorySlug}`), not the resolved URL, so exact-string
     * membership fails for any parameterised route. The previous check
     * `currentRoute in bottomNavRoutes` hid the bar when on
     * `spin/{categorySlug}` (the Spin screen WITH a category), which is
     * exactly the user-visible splash-nav bug.
     */
    val bottomNavRoutePrefixes: Set<String> = setOf(HOME, SPIN, CABINET, REVEAL)

    /**
     * Route PREFIXES that own navigation during app boot (splash → home /
     * onboarding / crash gate). The NavHost waits for these to finish before
     * acting on a deep-linked entry open.
     */
    val bootGatePrefixes: Set<String> = setOf(SPLASH, ONBOARDING, CRASH)
}

/**
 * Standard bottom-nav tab switch: pop the back stack back to the persistent
 * HOME root (saving the popped states), then navigate to [route] with
 * `launchSingleTop` + `restoreState` so every tab keeps at most one entry on
 * the back stack while preserving its scroll/UI state across switches.
 *
 * Anchored to HOME — NOT `graph.findStartDestination()`: the NavHost's
 * declared start destination (SPLASH) is popped inclusively on launch, so
 * `popUpTo(findStartDestination())` would target a destination that is no
 * longer in the stack — a silent no-op that lets every tab switch and every
 * re-opened screen pile up duplicate back-stack entries (back then walks
 * through the same screens repeatedly). HOME is the persistent root that
 * always remains after Splash/Onboarding/Crash routes land.
 */
fun NavController.navigateToTab(route: String) {
    // From a pushed/parameterized destination (e.g. "spin/artists" opened
    // from a Quests passport stamp or a discovery daily's Go), pop back to
    // the HOME root explicitly FIRST: the popUpTo(HOME)+launchSingleTop
    // navigate can cancel itself there — after the pop, HOME is already the
    // top entry, so singleTop skips the navigate and the tap appears dead
    // (the user had to back out to Quests before a tab tap would respond).
    // Landing on HOME first makes every tab tap from a pushed screen switch.
    val current = currentBackStackEntry?.destination?.route
    // v20 — a bottom-nav route PUSHED on top of a non-tab screen (e.g. the
    // Cabinet opened from Profile) is a pushed instance too, even though its
    // route name matches a tab. A genuine tab entry always sits directly on
    // the HOME root; anything else — a pushed screen, or a tab route with a
    // pushed screen beneath it — gets the explicit pop so the tap never
    // looks dead.
    val below = previousBackStackEntry?.destination?.route
    val genuineTabInstance = current in CurioRoutes.bottomNavRoutes &&
        below == CurioRoutes.HOME
    if (current != null && !genuineTabInstance) {
        popBackStack(CurioRoutes.HOME, inclusive = false)
    }
    navigate(route) {
        popUpTo(CurioRoutes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Standard quest / guided-tour jump: TAB routes switch tabs (pop to HOME +
 * save state, like [navigateToTab]); everything else is a push destination
 * (`launchSingleTop`). Used by the Quests page's Go buttons and by the quest
 * tour's auto-navigation.
 */
fun NavController.navigateToQuestRoute(route: String) {
    if (route in CurioRoutes.bottomNavRoutePrefixes) {
        navigateToTab(route)
    } else {
        navigate(route) { launchSingleTop = true }
    }
}
