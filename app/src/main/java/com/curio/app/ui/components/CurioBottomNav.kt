package com.curio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToTab
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.pastelFillInk

/**
 * Out-of-band handoff for the Spin page's category tint wash — published by
 * [com.curio.app.features.spin.SpinScreen] and consumed by
 * [CurioNavigationRail] so the rail can blend with the tinted Spin page
 * (the rail lives outside the NavHost content and can't read SpinScreen's
 * state directly). Mirrors the [com.curio.app.navigation.LightboxTarget] pattern.
 *
 * Spin publishes its deck wash; Cabinet publishes its active-filter wash
 * (null when showing "All" — a plain page). Home publishes its category
 * tint wash only when the "Home tint" experiment is on; the bar falls back
 * to the theme surface whenever the active route publishes no wash.
 */
object CurioNavTint {
    var spinWash by mutableStateOf<Color?>(null)
        private set
    // Cabinet's active-filter category wash — published by CabinetScreen so
    // the nav bar blends with the tinted Cabinet page while a category filter
    // is active. Null when the Cabinet shows "All" (plain page) or isn't
    // composed.
    var cabinetWash by mutableStateOf<Color?>(null)
        private set
    // Home's page background — published by HomeScreen (the lane wash when
    // "Hero follows Spin lane" is active, otherwise the rose-tinted default)
    // so the nav bar blends with the Home page in every mode. Never null
    // while Home is composed.
    var homeWash by mutableStateOf<Color?>(null)
        private set
    // v149 — per-tab ACCENTS: the floating pill bar's ACTIVE pill wears the
    // current page's category color (the Spin lane's accent, the Cabinet's
    // active-filter accent, Home's rose) instead of the static secondary.
    // Null = the page publishes none → the pill falls back to secondary.
    var spinAccent by mutableStateOf<Color?>(null)
        private set
    var cabinetAccent by mutableStateOf<Color?>(null)
        private set
    var homeAccent by mutableStateOf<Color?>(null)
        private set

    fun publishSpinWash(color: Color?) {
        spinWash = color
    }

    fun publishCabinetWash(color: Color?) {
        cabinetWash = color
    }

    fun publishHomeWash(color: Color?) {
        homeWash = color
    }

    fun publishSpinAccent(color: Color?) {
        spinAccent = color
    }

    fun publishCabinetAccent(color: Color?) {
        cabinetAccent = color
    }

    fun publishHomeAccent(color: Color?) {
        homeAccent = color
    }
}

/**
 * v135 — Home drawer visibility, published by HomeScreen so the NavHost can
 * hide the floating pill bar while the drawer is open: the drawer must sit
 * ABOVE the nav bar (it covers the whole screen), so the bar yields while
 * the drawer is up and returns when it closes.
 * v147 — the drawer now lives AT THE NAVHOST ROOT, drawn ABOVE the floating
 * pill bar while the bar stays composed underneath (no more hide-and-
 * reappear): Home's hamburger calls [requestOpen], the NavHost owns the
 * actual DrawerState and observes the request, and [isOpen] tracks the real
 * open state (published by the NavHost).
 */
object CurioDrawerState {
    var isOpen by mutableStateOf(false)
        private set

    // Bumped on every open request so the NavHost's LaunchedEffect re-fires
    // even when a second request arrives while the drawer is already open.
    private var openTick by mutableStateOf(0)

    /** Read by the NavHost: changes whenever Home requests the drawer open. */
    val openRequest: Int
        get() = openTick

    fun publishOpen(open: Boolean) {
        isOpen = open
    }

    fun requestOpen() {
        openTick++
    }
}

/**
 * v142 — one-shot "open the Spin category picker" request. Home's first-run
 * "Pick a lane" sets [pending] before navigating to the Spin tab, and
 * SpinScreen consumes it (opens its CategoryPickerSheet — the same lane
 * chips + Mix presets the deck uses) when it observes it. Lives here with
 * the other cross-screen UI state (see [CurioDrawerState]).
 */
object SpinPickerRequest {
    var pending by mutableStateOf(false)
}

/**
 * Curio's persistent bottom navigation — see Curio navigation contract.
 *
 * Three destinations:
 *   [ Home ]   [ Shuffle ]   [ Cabinet ]
 *
 * Tapping a tab uses the standard Compose pattern: navigate with
 * popUpTo(startDestination) + saveState=true + restoreState=true +
 * launchSingleTop=true. This preserves each tab's back stack across
 * switches and avoids re-creating the screen UI from scratch.
 *
 * The bar is hidden outside of [CurioRoutes.bottomNavRoutes] by the
 * parent scaffold (see CurioNavHost). This composable assumes it IS visible.
 */
data class CurioBottomDestination(
    val route: String,
    val label: String,
    val icon: String,
    val selectedIcon: String = icon
)

object CurioBottomNavItems {
    val Home = CurioBottomDestination(
        route = CurioRoutes.HOME,
        label = "Home",
        icon = CurioIcons.Home
    )
    val Shuffle = CurioBottomDestination(
        route = CurioRoutes.SPIN,
        label = "Shuffle",
        icon = CurioIcons.AutoAwesome,
        selectedIcon = CurioIcons.AutoAwesome
    )
    val Cabinet = CurioBottomDestination(
        route = CurioRoutes.CABINET,
        label = "Cabinet",
        icon = CurioIcons.Inventory2,
        selectedIcon = CurioIcons.Inventory2
    )

    val all: List<CurioBottomDestination> = listOf(Home, Shuffle, Cabinet)
}

// v124 — the phone bottom nav is a FLOATING PILL BAR: every tab renders
// icon-only, and the active tab smoothly expands to reveal its label
// (spring width morph + label slide-out) while the previously active pill
// collapses back to an icon. The active indicator fills the WHOLE pill
// (icon + label), not just the icon. Colors are pure colorScheme tokens
// (surfaceContainerHigh pill + v131 SOLID secondary indicator +
// onSecondary ink), so the bar follows the Curio / AMOLED / Material
// (dynamic) themes and dark mode out of the box.
// v131 — the pills grew a touch (52dp tall/icons, 112dp expanded).
// v151 — the user asked for a LARGER bottom pill: 60dp icon/height and
// 128dp expanded so the bar reads proper and the label has real room.
// v159 — the pill got SLIMMER: same 60/128dp widths (the length is what
// the user wanted kept) but the height dropped 60 → 48dp so the bar reads
// shorter; the 26dp icon still breathes inside.
private val FloatingPillIconWidth = 60.dp
private val FloatingPillExpandedWidth = 128.dp
private val FloatingPillHeight = 48.dp

/**
 * Curio's persistent bottom navigation — a floating pill bar (v124).
 *
 * Three destinations:
 *   [ Home ]   [ Shuffle ]   [ Cabinet ]
 *
 * Tapping a tab uses the standard Compose pattern: navigate with
 * popUpTo(startDestination) + saveState=true + restoreState=true +
 * launchSingleTop=true. This preserves each tab's back stack across
 * switches and avoids re-creating the screen UI from scratch.
 *
 * The bar is hidden outside of [CurioRoutes.bottomNavRoutes] by the
 * parent NavHost (see CurioNavHost). This composable assumes it IS visible.
 * v129 — no Scaffold slot and no painted band: the bar is a floating
 * overlay drawn ON TOP of the page's own full-bleed background (the page
 * clears it with bottom padding). It sizes to the pill row, floats above
 * the gesture bar via [androidx.compose.foundation.layout.navigationBarsPadding]
 * with a 12dp air gap, and the page shows through around it — no strip.
 * Wide windows use [CurioNavigationRail] instead.
 */
@Composable
fun CurioFloatingNavBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val routePrefix = currentRoute?.substringBefore("/")
    // Reveal is entered from the Shuffle deck, so keep Shuffle selected while
    // the reveal page is open instead of leaving every tab unselected.
    val selectedRoute = if (routePrefix == CurioRoutes.REVEAL.substringBefore("/")) {
        CurioRoutes.SPIN
    } else {
        routePrefix
    }

    // v149 — the ACTIVE pill wears the current page's category color
    // (published via [CurioNavTint]); null on plain pages → secondary.
    val pageAccent = curioNavActiveAccent(selectedRoute)

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            // v149 — the container follows the page tint dynamically
            // (animated, theme-aware) while staying elevated.
            // v157 — the dark-mode hairline rim is GONE (the user asked):
            // the capsule stays defined by its elevated fill alone.
            color = curioFloatingNavContainer(routePrefix),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CurioBottomNavItems.all.forEach { destination ->
                    // The hierarchy walk handles nested-graph destinations;
                    // today all routes are flat so the hierarchy contains
                    // exactly the current route + start destination.
                    val selected = selectedRoute == destination.route ||
                        navBackStackEntry?.destination?.hierarchy?.any { routeEntry ->
                            routeEntry.route?.substringBefore("/") == destination.route
                        } == true
                    FloatingNavPill(
                        destination = destination,
                        selected = selected,
                        activeAccent = pageAccent,
                        onClick = {
                            // Compare the route PREFIX: the Shuffle tab is also
                            // the current screen when the deck was opened via a
                            // category launch ("spin/artists"), and re-tapping an
                            // already-selected tab must be a no-op instead of
                            // re-opening it.
                            if (selectedRoute != destination.route) {
                                // Anchor to HOME (the persistent root), not the
                                // graph start destination: SPLASH is popped on
                                // launch, so popUpTo(startDestination) would be
                                // a no-op and tab switches would pile up dupes.
                                navController.navigateToTab(destination.route)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * One pill in the floating bar: icon-only while inactive; the selected
 * pill springs wider and slides its label out (the indicator covers the
 * whole pill, icon + label). v129 — BOTH pills animate with the SAME
 * spring: the deselected pill shrinks at exactly the rate the newly
 * selected one grows, so the bar's total width never dips and it never
 * re-centers (the "squeeze" the old snap-close + springy-open caused).
 * The label exit stays instant (the closing pill's text vanishes, per
 * v125: "only the active pill text has the morph open animation").
 */
@Composable
private fun FloatingNavPill(
    destination: CurioBottomDestination,
    selected: Boolean,
    // v149 — the current page's category accent (see [curioNavActiveAccent]);
    // null → the theme's PRIMARY (coral) with onPrimary ink (v161: the old
    // secondary/butter fallback read as a stray yellow on Cabinet "All").
    activeAccent: Color?,
    onClick: () -> Unit
) {
    val pillWidth by animateDpAsState(
        targetValue = if (selected) FloatingPillExpandedWidth else FloatingPillIconWidth,
        // v155 — damping 0.75 → 0.9: the old spring overshot and bounced on
        // settle ("clanky"); near-critical damping glides to rest.
        // v161 — stiffness MediumLow → Medium: the collapse (128 → 60dp)
        // used to drag for a full second; Medium settles crisply with no
        // overshoot at the same near-critical damping.
        animationSpec = spring(
            dampingRatio = 0.9f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floatingNavPillWidth"
    )
    // v149 — the active indicator follows the PAGE: when the page publishes
    // a category accent (Spin lane / Cabinet filter / Home rose) the active
    // pill wears it with the theme-aware fill-ink contract; plain pages
    // (Cabinet "All") fall back to the theme's PRIMARY (coral) — v161: the
    // old secondary fallback (butter yellow) looked like a stray yellow
    // pill on the plain Cabinet page. Never a translucent container.
    val activeFill = activeAccent ?: MaterialTheme.colorScheme.primary
    val activeInk = if (activeAccent != null) pastelFillInk(activeAccent)
                    else MaterialTheme.colorScheme.onPrimary
    // v155 — the fill fades in/out (alpha) synced to the width spring
    // instead of snapping on/off, and the icon tint crossfades — no hard
    // color pops on tab switch.
    val fillColor by animateColorAsState(
        targetValue = activeFill.copy(alpha = if (selected) 1f else 0f),
        animationSpec = spring(
            dampingRatio = 0.9f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "floatingNavPillFill"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) activeInk else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "floatingNavPillIconTint"
    )
    Box(
        modifier = Modifier
            .width(pillWidth)
            .height(FloatingPillHeight)
            .clip(RoundedCornerShape(50))
            .background(fillColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CurioIcon(
                name = if (selected) destination.selectedIcon else destination.icon,
                contentDescription = destination.label,
                tint = iconTint,
                size = 26.dp
            )
            // Enter keeps the slide-out morph for the newly active pill;
            // v155 — the label fade now tracks the pill's expansion
            // (tween 240 + FastOutSlowIn) instead of popping in at 160ms;
            // exit stays instant per v125 (the closing pill's text vanishes
            // the moment its pill is deselected).
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(tween(240, easing = FastOutSlowInEasing)),
                // v161 — the collapse used to VAPORIZE the label
                // (fadeOut(tween(0))) while the pill took a second to
                // shrink — a dead empty box deflating. The exit now glides
                // out (160ms) with the shrink so the deselected pill reads
                // as one smooth motion.
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) +
                    fadeOut(tween(160, easing = FastOutSlowInEasing))
            ) {
                Text(
                    text = destination.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = activeInk,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp, end = 2.dp)
                )
            }
        }
    }
}

/**
 * Curio's wide-window navigation — a slim NavigationRail on the left edge,
 * shown instead of the floating pill bar on medium/expanded windows
 * (tablets and landscape). Shares [CurioBottomNavItems] and the page-wash
 * tint, so the rail wears the same category-tinted container color as the
 * page it sits beside. The parent NavHost decides which nav chrome to
 * render (see CurioNavHost) and passes a full-height modifier.
 */
@Composable
fun CurioNavigationRail(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val routePrefix = currentRoute?.substringBefore("/")
    val selectedRoute = if (routePrefix == CurioRoutes.REVEAL.substringBefore("/")) {
        CurioRoutes.SPIN
    } else {
        routePrefix
    }
    // v161 — the rail's active indicator mirrors the phone pill bar: the
    // current page's category accent when published, else the theme PRIMARY
    // (the old hard-coded secondary read as a stray yellow on Cabinet "All").
    val pageAccent = curioNavActiveAccent(selectedRoute)
    val railActiveFill = pageAccent ?: MaterialTheme.colorScheme.primary
    val railActiveInk = if (pageAccent != null) pastelFillInk(pageAccent)
                        else MaterialTheme.colorScheme.onPrimary

    NavigationRail(
        modifier = modifier,
        containerColor = curioNavContainerColor(routePrefix),
        // The rail's default insets (systemBarsForVisualComponents, Vertical +
        // Start) keep its items clear of the status bar and gesture bar while
        // the rail surface itself spans the full window height.
        content = {
            Spacer(Modifier.height(10.dp))
            CurioBottomNavItems.all.forEach { destination ->
                val selected = selectedRoute == destination.route ||
                    navBackStackEntry?.destination?.hierarchy?.any { routeEntry ->
                        routeEntry.route?.substringBefore("/") == destination.route
                    } == true

                NavigationRailItem(
                    selected = selected,
                onClick = {
                    // Same prefix-based no-op guard as the bottom bar: a
                    // category-launched deck ("spin/artists") is still the
                    // Shuffle tab, so re-tapping must not re-navigate.
                    if (selectedRoute != destination.route) {
                        navController.navigateToTab(destination.route)
                    }
                },
                    icon = {
                        CurioIcon(
                            name = if (selected) destination.selectedIcon else destination.icon,
                            contentDescription = destination.label,
                            // v161 — active ink follows the page accent /
                            // primary fallback, matching the phone pill bar.
                            tint = if (selected) railActiveInk
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 24.dp
                        )
                    },
                    label = {
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = railActiveInk,
                        selectedTextColor = railActiveInk,
                        indicatorColor = railActiveFill,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    )
}

/**
 * Shared nav-chrome container color: each tab's category-tinted page wash
 * (Spin's deck wash, the Cabinet's active-filter wash, Home's experiment
 * tint) matching the page background the user is looking at. Home stays on
 * the plain theme surface unless the "Home tint" experiment is on, and any
 * route that publishes no wash falls back to the surface too.
 */
@Composable
private fun curioNavContainerColor(routePrefix: String?): Color {
    val target = when (routePrefix) {
        // Fall back to the theme BACKGROUND (not surface): when a page
        // publishes no wash its own background IS `colorScheme.background`
        // (dark mode pitch black / light cream) — the nav slot must match
        // it exactly or the pill floats on a visible strip. v125.
        CurioRoutes.SPIN -> CurioNavTint.spinWash ?: MaterialTheme.colorScheme.background
        CurioRoutes.CABINET -> CurioNavTint.cabinetWash ?: MaterialTheme.colorScheme.background
        CurioRoutes.HOME -> CurioNavTint.homeWash ?: MaterialTheme.colorScheme.background
        else -> MaterialTheme.colorScheme.background
    }
    return animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 420),
        label = "curioNavContainerColor"
    ).value
}

/**
 * v149 — the FLOATING pill container (bottom nav bar, tour dock, pet
 * studio bar): the current page's wash LIFTED toward the elevated surface
 * so the pill follows the page tint dynamically while staying readable.
 * The rail uses the RAW wash because it's edge-anchored and blends into
 * the page; a floating pill that matched the page exactly would vanish
 * into it. Dark mode keeps the elevated surface unchanged — the pages are
 * near-black, so the wash adds nothing and the scheme's lightness steps
 * already read as lift. Animated with the same 420ms tween as the rail.
 * v155 — light mode was lifting 55% toward the elevated surface, which
 * washed the tint out to a plain parchment capsule (the active pill got
 * the dynamic color, the bar's background didn't). Now 30%, so the page
 * tint shows through the capsule while it still reads lifted above the
 * page.
 */
@Composable
internal fun curioFloatingNavContainer(routePrefix: String?): Color {
    if (isCurioDarkTheme()) return MaterialTheme.colorScheme.surfaceContainerHigh
    val wash = curioNavContainerColor(routePrefix)
    return lerp(wash, MaterialTheme.colorScheme.surfaceContainerHigh, 0.30f)
}

/**
 * v149 — the floating bar's ACTIVE pill color: the current page's category
 * accent (published by the tab screens via [CurioNavTint] — the Spin
 * lane's accent, the Cabinet's active-filter accent, Home's rose). Null on
 * plain pages (and non-tab routes) so the pill falls back to the theme
 * PRIMARY (v161 — was secondary/butter, which read as a stray yellow on
 * Cabinet "All").
 */
@Composable
private fun curioNavActiveAccent(routePrefix: String?): Color? = when (routePrefix) {
    CurioRoutes.SPIN -> CurioNavTint.spinAccent
    CurioRoutes.CABINET -> CurioNavTint.cabinetAccent
    CurioRoutes.HOME -> CurioNavTint.homeAccent
    else -> null
}
