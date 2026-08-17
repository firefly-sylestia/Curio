package com.curio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToTab
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons

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

    fun publishSpinWash(color: Color?) {
        spinWash = color
    }

    fun publishCabinetWash(color: Color?) {
        cabinetWash = color
    }

    fun publishHomeWash(color: Color?) {
        homeWash = color
    }
}

/**
 * v135 — Home drawer visibility, published by HomeScreen so the NavHost can
 * hide the floating pill bar while the drawer is open: the drawer must sit
 * ABOVE the nav bar (it covers the whole screen), so the bar yields while
 * the drawer is up and returns when it closes.
 */
object CurioDrawerState {
    var isOpen by mutableStateOf(false)
        private set

    fun publishOpen(open: Boolean) {
        isOpen = open
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
// v131 — the pills grew a touch (52dp tall/icons, 112dp expanded) so the
// bar reads chunkier and the active tab's label has more room.
private val FloatingPillIconWidth = 52.dp
private val FloatingPillExpandedWidth = 112.dp
private val FloatingPillHeight = 52.dp

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

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
    onClick: () -> Unit
) {
    val pillWidth by animateDpAsState(
        targetValue = if (selected) FloatingPillExpandedWidth else FloatingPillIconWidth,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "floatingNavPillWidth"
    )
    // v131 — the active indicator is a SOLID secondary fill with its on-color
    // ink (the v27q selection contract — never a translucent container), so
    // the active tab reads as a defined amber pill in light AND dark instead
    // of the washed-out translucent container it wore before.
    val activeInk = MaterialTheme.colorScheme.onSecondary
    Box(
        modifier = Modifier
            .width(pillWidth)
            .height(FloatingPillHeight)
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.secondary
                else Color.Transparent
            )
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
                tint = if (selected) activeInk else MaterialTheme.colorScheme.onSurfaceVariant,
                size = 24.dp
            )
            // Enter keeps the slide-out morph for the newly active pill;
            // exit is tween(0) so the label VANISHES the moment its pill is
            // deselected (no closing shrink animation).
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(tween(160)),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(tween(0))
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
                            // v131 — solid secondary indicator + on-secondary
                            // ink, matching the phone pill bar.
                            tint = if (selected)
                                MaterialTheme.colorScheme.onSecondary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
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
                        selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                        selectedTextColor = MaterialTheme.colorScheme.onSecondary,
                        indicatorColor = MaterialTheme.colorScheme.secondary,
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
