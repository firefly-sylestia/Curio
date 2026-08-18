package com.curio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.curio.app.data.AppPreferences
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToTab
import com.curio.app.ui.theme.ChangaOneFontFamily
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.materialThemeOn
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.toHsl

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
// v184 — the user asked for the pill "a little wide" and "a little
// higher": icon pills 60 → 64dp, expanded 128 → 136dp, height 48 → 52dp.
private val FloatingPillIconWidth = 64.dp
private val FloatingPillExpandedWidth = 136.dp
// v201 — the leave-hold collapse pulls the pill TIGHTER than its resting
// icon width (64 → 44dp): just the icon + a sliver of breathing room, so
// the pill visibly cinches in before the bar unmounts instead of stopping
// at its idle size ("collapse even more").
private val FloatingPillCollapsedWidth = 44.dp
private val FloatingPillHeight = 52.dp

// v162 — ONE spring family drives EVERY animated property of the pill
// (width, active fill, icon tint, label expand/shrink). Before, the width
// ran Medium while the FILL still ran the old MediumLow and the label /
// icon tint ran their own 240/160ms tweens — the fill lagged the width
// and the label finished ~3x early, which is what still read as janky
// after v161. Identical spring params = identical trajectories from the
// same start frame = perfect lockstep. v165 — the specs are typed per
// animated value: the generic must match the target type (spring<Color>
// for color animations, spring<IntSize> for the label's expand/shrink,
// spring<Float> for fades); the physics are the same so the lockstep
// holds across all four.
// v166 — the pill family runs SLOWER and CRITICALLY damped. Stiffness
// Medium (1500) snapped the collapse shut in a beat — the violence the
// user flagged. The family now runs 400 (v173 — even slower than the v166
// 750, which the user still called "too rapid") at damping 1.0, so the
// width/fill/label glide to rest with ZERO overshoot or bounce: smooth,
// never violent. Same physics across all four specs keeps the v162/v165
// lockstep (every element finishes with the pill).
// v184 — "even smoother and calmer": stiffness 400 → 240 (~40% slower
// settle). Still critically damped (damping 1.0 = zero overshoot, zero
// bounce) — the calmest glide a spring can give; identical physics across
// all four specs keeps the lockstep.
// v201 — "make the home nav pill collapse even smoother… collape even
// more": stiffness 240 → 150 (~40% slower settle again, the longest calm
// glide a critically-damped spring can give), and the leave-hold collapse
// now targets a width TIGHTER than the resting icon pill (see
// [FloatingNavPill]) so the pill visibly pulls in before the bar
// unmounts. The NavHost hold is sized to this family's settle time.
// v206 — "the collapse of home nav pil can be more smoother": 150 → 120
// (~20% slower still), the calmest glide yet; the NavHost hold extends to
// ~460ms to match (see CurioNavHost).
private val PillWidthSpring = spring<Dp>(dampingRatio = 1f, stiffness = 120f)
private val PillMotionSpring = spring<Float>(dampingRatio = 1f, stiffness = 120f)
private val PillColorSpring = spring<Color>(dampingRatio = 1f, stiffness = 120f)
private val PillExpandSpring = spring<IntSize>(dampingRatio = 1f, stiffness = 120f)

// v208e — how long the collapsed pill bar stays composed after the route
// leaves the tab set, then unmounts. Tuned to the Topic Reveal's
// Like/Dislike entrance: the pill slides in over 220ms at its NATURAL
// time, and the bar vanishes right as it lands (240 = 220 + a hair), so
// the nav pill syncs TO the pill — the collapse motion is still visible
// beneath it (overlap), then the bar is gone the moment the pill arrives.
// The reveal's pill renders ABOVE the bar via the [SentimentPillHost]
// overlay, so the overlap reads clean.
// (Long — it feeds kotlinx.coroutines.delay(), which takes Long millis; a
// literal 460 compiled because literals widen, a typed Int const does not.)
const val FloatingNavCollapseHoldMillis = 240L

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
    modifier: Modifier = Modifier,
    // v194 — while the bar is in its leave-hold phase (the route left the
    // tab set but the bar stays composed so the selected pill can COLLAPSE
    // with its spring), NO pill stays selected — they all glide closed. The
    // old code forced SPIN selected on the reveal route, so leaving Home for
    // a topic reveal made the SPIN pill POP OPEN during the hold and the bar
    // then vanished with a pill stuck expanded ("neither it collapse").
    collapsing: Boolean = false
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val routePrefix = currentRoute?.substringBefore("/")
    // Reveal is entered from the Shuffle deck, so keep Shuffle selected while
    // the reveal page is open instead of leaving every tab unselected — but
    // only while the bar is actually ON a page (see [collapsing]).
    val selectedRoute = if (collapsing) {
        null
    } else if (routePrefix == CurioRoutes.REVEAL.substringBefore("/")) {
        CurioRoutes.SPIN
    } else {
        routePrefix
    }
    // v188 — a light tick when switching tabs.
    val haptics = LocalHapticFeedback.current

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
                // v184 — more breathing room: bar padding 7 → 8dp and pill
                // gap 6 → 10dp so the inactive pills sit with a little more
                // space between them.
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        // v201 — during the leave-hold the pill cinches below
                        // its idle icon width (see [FloatingPillCollapsedWidth]).
                        collapsing = collapsing,
                        activeAccent = pageAccent,
                        onClick = {
                            // Compare the route PREFIX: the Shuffle tab is also
                            // the current screen when the deck was opened via a
                            // category launch ("spin/artists"), and re-tapping an
                            // already-selected tab must be a no-op instead of
                            // re-opening it.
                            if (selectedRoute != destination.route) {
                                // v188 — light tick on tab switch.
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
 * v184 — the collapse mirrors the expand: the outgoing pill's width
 * shrinks and its label slides back into the icon on the SAME springs as
 * the incoming pill's growth (the "instant label exit" of v125 is long
 * gone — v162 routed the label through the shared spring family, so the
 * closing pill glides closed exactly as the opening one glides open).
 */
@Composable
private fun FloatingNavPill(
    destination: CurioBottomDestination,
    selected: Boolean,
    // v201 — true only during the bar's leave-hold: the pill cinches to
    // [FloatingPillCollapsedWidth] (tighter than the idle icon pill) so
    // the collapse visibly finishes before the bar unmounts.
    collapsing: Boolean,
    // v149 — the current page's category accent (see [curioNavActiveAccent]);
    // null → the theme's PRIMARY (coral) with onPrimary ink (v161: the old
    // secondary/butter fallback read as a stray yellow on Cabinet "All").
    activeAccent: Color?,
    onClick: () -> Unit
) {
    val pillWidth by animateDpAsState(
        targetValue = if (selected) FloatingPillExpandedWidth
        else if (collapsing) FloatingPillCollapsedWidth
        else FloatingPillIconWidth,
        // v162 — the shared [PillWidthSpring] (near-critical 0.9 + Medium),
        // identical to the fill/icon/label springs so they stay in lockstep.
        animationSpec = PillWidthSpring,
        label = "floatingNavPillWidth"
    )
    // v166 — the active indicator follows the PAGE: when the page publishes
    // a category accent (Spin lane / Cabinet filter / Home rose) the active
    // pill wears it CALMED (saturation pulled in light mode — the muted
    // look the user asked for); plain pages (Cabinet "All") fall back to
    // the theme's MUTED secondaryContainer, NOT the coral primary — the
    // v161→v166 history: solid butter read as a stray yellow, coral read as
    // a stray pink (same pink family as the spin shuffle brand) — the soft
    // warm container reads right in both themes. See [curioActivePillFill].
    val activeFill = curioActivePillFill(activeAccent)
    val activeInk = curioActivePillInk(activeAccent)
    // v162 — the fill fades with the SAME spring as the width (before v162
    // it still ran the old MediumLow spring and lagged the pill), and the
    // icon tint crossfades on the same spring too (it used to finish in
    // 200ms while the pill kept moving) — no hard pops, no out-of-step
    // elements.
    val fillColor by animateColorAsState(
        targetValue = activeFill.copy(alpha = if (selected) 1f else 0f),
        animationSpec = PillColorSpring,
        label = "floatingNavPillFill"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) activeInk else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = PillColorSpring,
        label = "floatingNavPillIconTint"
    )
    // v167 — NO tap ripple: the pill's click uses a null indication, so
    // tapping a tab never flashes the grey ripple circle (the user called
    // it "the touch shadow in nav bar"). The interactionSource is still
    // remembered so the semantics/click remain fully functional.
    val pillInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .width(pillWidth)
            .height(FloatingPillHeight)
            .clip(RoundedCornerShape(50))
            .background(fillColor)
            .clickable(
                interactionSource = pillInteraction,
                indication = null,
                onClick = onClick
            ),
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
            // v162 — the label's expand/shrink + fade run the SAME spring
            // as the pill width (AnimatedVisibility's own tweens used to
            // finish 3x early, so the label was fully in/out while the pill
            // was still mid-flight). Now the slide-out, the fade and the
            // width all move as ONE piece.
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(PillExpandSpring, expandFrom = Alignment.Start) + fadeIn(PillMotionSpring),
                exit = shrinkHorizontally(PillExpandSpring, shrinkTowards = Alignment.Start) + fadeOut(PillMotionSpring)
            ) {
                Text(
                    text = destination.label,
                    // v184 — the tab labels now wear the bundled Changa One
                    // display face (chunky single-weight — pair it with
                    // Normal so no fake-bold synthesis; the glyphs are
                    // already display-heavy). Size nudged 12 → 13sp so the
                    // wide face reads at the same visual weight as the old
                    // geom Bold.
                    // v186 — the user asked for the labels EVEN LARGER in
                    // the default look: 13 → 15sp (still fits the 136dp
                    // expanded pill: icon 26 + label ~70sp + padding).
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = ChangaOneFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp
                    ),
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
    // v188 — light tick on tab switch (wide-window rail).
    val haptics = LocalHapticFeedback.current
    // v161/v166 — the rail's active indicator mirrors the phone pill bar:
    // the current page's accent CALMED (v166 muted the bright accents), else
    // the theme's muted secondaryContainer (the old hard-coded secondary /
    // primary fallbacks read as stray yellow / pink on Cabinet "All").
    val pageAccent = curioNavActiveAccent(selectedRoute)
    val railActiveFill = curioActivePillFill(pageAccent)
    val railActiveInk = curioActivePillInk(pageAccent)

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
                        // v188 — light tick on tab switch.
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                            // v184 — matches the pill bar's Changa One tab
                            // labels (Normal — the single-weight display face
                            // needs no fake bold).
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ChangaOneFontFamily,
                                fontWeight = FontWeight.Normal
                            )
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
    // v190 — Material theme: the M3 nav container role (neutral surface) —
    // the page wash collapses (M3: navigation lives on surfaceContainer,
    // not per-page tints).
    if (materialThemeOn) return MaterialTheme.colorScheme.surfaceContainer
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
internal fun curioFloatingNavContainer(routePrefix: String?): Color =
    curioFloatingNavContainerFor(curioNavContainerColor(routePrefix))

/**
 * v167 — the floating capsule color for an EXPLICIT page wash (the reveal
 * Like/Dislike pill passes the reveal page's own category wash, which the
 * route-keyed [curioFloatingNavContainer] can't reach — the reveal isn't a
 * tab route). Same lift rule as the nav bar: light mode lifts the wash 30%
 * toward the elevated surface so the pill follows the page tint while
 * staying readable; dark keeps the elevated surface (near-black pages).
 */
@Composable
internal fun curioFloatingNavContainerFor(wash: Color): Color {
    // v190 — Material theme: the M3 nav container role instead of the
    // page-tinted lift.
    if (materialThemeOn) return MaterialTheme.colorScheme.surfaceContainer
    if (isCurioDarkTheme()) return MaterialTheme.colorScheme.surfaceContainerHigh
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
    // v173 — Cabinet "All" (no active filter) inherits the SPIN screen's
    // accent ("blue or red or whatever the spin screen color have set"),
    // not the muted butter fallback that read as a stray yellow. If Spin
    // hasn't published yet, falls back to the theme primary — the coral
    // brand, which IS the default wildcard deck's own accent.
    CurioRoutes.CABINET -> CurioNavTint.cabinetAccent
        ?: (CurioNavTint.spinAccent ?: MaterialTheme.colorScheme.primary)
    CurioRoutes.HOME -> CurioNavTint.homeAccent
    else -> null
}

/**
 * v166 — the ACTIVE indicator's fill. The page accent is CALMED before it
 * paints: light mode pulls saturation ~45% (hue + lightness preserved) so
 * the loud lane colors read muted, not neon — the bright saturated accents
 * were exactly the "bright colors" the user wanted toned down. Dark mode
 * keeps the theme's deep jewel tone (already muted by design) and pastel
 * mode keeps the airy pastel twin (already calm). Null (Home without a
 * published hero tint, non-tab routes) falls back to the theme's MUTED
 * default container: secondaryContainer (soft warm butter at low alpha,
 * light + dark aware) with its proper [curioActivePillInk]. v173 —
 * Cabinet "All" no longer reaches this fallback: it inherits the SPIN
 * deck's accent (see [curioNavActiveAccent]), so the stray-yellow path is
 * Home-only now.
 */
@Composable
// v208 — internal (not private): the Spin experiment reuses the nav pill's
// CALMED accent fill + ink for the nav-style Categories/Filter buttons.
internal fun curioActivePillFill(accent: Color?): Color {
    // v190 — Material theme: the M3 navigation indicator — the scheme's
    // muted secondaryContainer (no per-lane colors in the bar; user:
    // "fix the nav bar material color as they are bad").
    if (materialThemeOn) return MaterialTheme.colorScheme.secondaryContainer
    if (accent != null) {
        if (!isCurioDarkTheme() && !AppPreferences.pastelColorsState) {
            val a = toHsl(accent)
            return fromHsl(a.h, (a.s * 0.55f).coerceAtMost(0.55f), a.l)
        }
        return accent
    }
    return MaterialTheme.colorScheme.secondaryContainer
}

/** Ink that pairs with [curioActivePillFill] — the accent's pastel-aware
 *  deep/light twin on a category fill, the theme's onSecondaryContainer on
 *  the plain-page fallback (proper M3 pair, guaranteed contrast). */
@Composable
// v208 — internal (see [curioActivePillFill]).
internal fun curioActivePillInk(accent: Color?): Color {
    // v190 — Material theme: onSecondaryContainer on the M3 indicator.
    if (materialThemeOn) return MaterialTheme.colorScheme.onSecondaryContainer
    if (accent != null) return pastelFillInk(accent)
    return MaterialTheme.colorScheme.onSecondaryContainer
}
