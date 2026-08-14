package com.curio.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * [com.curio.app.features.spin.SpinScreen] and consumed by [CurioBottomBar]
 * so the Scaffold-level nav bar can blend with the tinted Spin page (the bar
 * lives outside the NavHost content and can't read SpinScreen's state
 * directly). Mirrors the [com.curio.app.navigation.LightboxTarget] pattern.
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
    // Home's category-tint wash — published by HomeScreen when the "Home
    // tint" experiment is on so the nav bar blends with the tinted Home
    // page. Null when the experiment is off (Home stays plain).
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

@Composable
fun CurioBottomBar(
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

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
        containerColor = curioNavContainerColor(routePrefix),
        tonalElevation = 0.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        CurioBottomNavItems.all.forEach { destination ->
            // The hierarchy walk handles nested-graph destinations; today all routes are flat
            // so the hierarchy contains exactly the current route + start destination.
            val selected = selectedRoute == destination.route ||
                navBackStackEntry?.destination?.hierarchy?.any { routeEntry ->
                    routeEntry.route?.substringBefore("/") == destination.route
                } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    // Compare the route PREFIX: the Shuffle tab is also the
                    // current screen when the deck was opened via a category
                    // launch ("spin/artists"), and re-tapping an already-
                    // selected tab must be a no-op instead of re-opening it.
                    if (selectedRoute != destination.route) {
                        // Anchor to HOME (the persistent root), not the
                        // graph start destination: SPLASH is popped on
                        // launch, so popUpTo(startDestination) would be a
                        // no-op and tab switches would pile up duplicates.
                        navController.navigateToTab(destination.route)
                    }
                },
                icon = {
                    CurioIcon(
                        name = if (selected) destination.selectedIcon else destination.icon,
                        contentDescription = destination.label,
                        tint = if (selected)
                            MaterialTheme.colorScheme.onSecondaryContainer
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
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Curio's wide-window navigation — a slim NavigationRail on the left edge,
 * shown instead of the bottom bar on medium/expanded windows (tablets and
 * landscape). Shares [CurioBottomNavItems] and the page-wash tint with
 * [CurioBottomBar], so the rail wears the same category-tinted container
 * color as the page it sits beside. The parent NavHost decides which nav
 * chrome to render (see CurioNavHost) and passes a full-height modifier.
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
                            tint = if (selected)
                                MaterialTheme.colorScheme.onSecondaryContainer
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
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
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
        CurioRoutes.SPIN -> CurioNavTint.spinWash ?: MaterialTheme.colorScheme.surface
        CurioRoutes.CABINET -> CurioNavTint.cabinetWash ?: MaterialTheme.colorScheme.surface
        CurioRoutes.HOME -> CurioNavTint.homeWash ?: MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surface
    }
    return animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 420),
        label = "curioNavContainerColor"
    ).value
}
