package com.curio.app.ui.pet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

/**
 * Landmark registry (v8.16) — lets the floating pet be SMART about its
 * wander instead of picking a purely random point every beat. Screens
 * publish a few "interesting things" (the spin button, the profile avatar,
 * a heading) with their window bounds; the pet walks TO them sometimes,
 * pokes them, and they react with a tiny springy pulse. Nothing here moves
 * or resizes any UI — landmarks are bounds only, published via
 * onGloballyPositioned and read by the pet overlay.
 *
 * Screen scoping: landmarks are keyed by the route PREFIX ("home", "spin",
 * "profile", …) so navigating clears one screen's set and the pet only ever
 * sees the current screen's things. Registration is a snapshot-state map, so
 * the pet overlay recomposes when a screen's landmarks change.
 */
object PetLandmarks {

    /** How the pet behaves around a landmark — and how the thing reacts. */
    enum class Kind {
        /** A button/gadget — the pet dashes in eagerly and boops it. */
        FUN,
        /** A text/reading thing — the pet tiptoes over and reads it. */
        CURIOUS,
        /**
         * v8.17 — a special spot (e.g. the pet's own flower bed): the pet
         * dashes over, pokes it, and does a little happy jig — a squish
         * bounce, a play-bow and a twirl.
         */
        PLAY
    }

    data class Landmark(
        val id: String,
        val kind: Kind,
        /** Window coordinates — matches the floating pet overlay's space. */
        val bounds: Rect
    )

    private val byScreenMap = mutableStateMapOf<String, List<Landmark>>()
    private val reactCounters = mutableStateMapOf<String, Int>()
    // v8.20 — a landmark being drag-hovered as a drop target (the flower
    // bed) glows so the drop is legible.
    private val hoveredIds = mutableStateMapOf<String, Boolean>()
    // v8.21 — a bottom drawer (the Spin filter / category sheets) is open on
    // a screen. The sheets live in a SEPARATE window layer, so their content
    // can't be tracked as landmarks — instead they report open/close here
    // and the pet hurries over to peek at the drawer from the screen edge.
    private val sheetOpenScreens = mutableStateMapOf<String, Boolean>()

    /** Landmarks for every screen (route prefix → list). Reactive. */
    val byScreen: Map<String, List<Landmark>> get() = byScreenMap

    /** Landmarks for one screen, or empty. */
    fun forScreen(screen: String?): List<Landmark> =
        screen?.let { byScreenMap[it].orEmpty() } ?: emptyList()

    /** How many times [id] has been poked — drives the landmark's pulse. */
    fun reactCount(id: String): Int = reactCounters[id] ?: 0

    /** The pet poked [id] — the landmark springs a beat. */
    fun poke(id: String) {
        reactCounters[id] = (reactCounters[id] ?: 0) + 1
    }

    /**
     * v8.19 — drop the poke counter for [id]. Called when a landmark leaves
     * composition, so RE-ENTERING the screen composes at count 0 instead of
     * pulsing once with a stale count from a previous visit.
     */
    fun resetReactCount(id: String) {
        reactCounters.remove(id)
    }

    /** Whether [id] is currently drag-hovered as a drop target (the bed). */
    fun isHovered(id: String): Boolean = hoveredIds[id] == true

    /**
     * v8.20 — the user is dragging the pet over [id] (or released it): the
     * landmark glows while true and stops when false. No-op when unchanged
     * (the drag loop reports every frame).
     */
    fun setHovered(id: String, hovered: Boolean) {
        if (hovered) {
            if (hoveredIds[id] != true) hoveredIds[id] = true
        } else {
            hoveredIds.remove(id)
        }
    }

    /**
     * v8.21 — the sheets report when a bottom drawer opens/closes on their
     * screen ("spin", "picker"). The pet reads [isSheetOpen] in its wander
     * loop and comes over to peek.
     */
    fun noteSheet(screen: String, open: Boolean) {
        if (open) sheetOpenScreens[screen] = true else sheetOpenScreens.remove(screen)
    }

    /** Whether a bottom drawer is currently open on [screen]. */
    fun isSheetOpen(screen: String?): Boolean =
        screen?.let { sheetOpenScreens[it] == true } ?: false

    /**
     * Add/refresh one landmark — replaces any previous one with the same id.
     * Compare-and-set: onGloballyPositioned fires on every layout pass, and
     * an unchanged landmark must NOT rewrite the map (each write invalidates
     * snapshot readers for zero change).
     */
    fun upsert(screen: String, landmark: Landmark) {
        val current = byScreenMap[screen].orEmpty()
        if (current.firstOrNull { it.id == landmark.id } == landmark) return
        byScreenMap[screen] = current.filterNot { it.id == landmark.id } + landmark
    }

    /** Forget a landmark when its composable leaves composition. */
    fun remove(screen: String, id: String) {
        val current = byScreenMap[screen].orEmpty()
        byScreenMap[screen] = current.filterNot { it.id == id }
    }
}

/**
 * Marks a piece of UI as a pet landmark (v8.16). ZERO layout impact: it
 * hands a modifier (bounds tracking + a tiny poke pulse) to the wrapped
 * content, which applies it to its own root — no extra Box, no size change.
 *
 * The pet's wander loop occasionally targets this landmark, walks over with
 * a kind-appropriate gait, pokes it ([PetLandmarks.poke]), and the landmark
 * springs a beat — the UI "reacts" without moving or re-laying out.
 *
 * @param id unique id within the screen (e.g. "spin", "avatar", "greeting")
 * @param kind how the pet behaves around it (FUN = boop, CURIOUS = read)
 * @param screen the route prefix this screen registers under
 */
@Composable
fun PetLandmark(
    id: String,
    kind: PetLandmarks.Kind,
    screen: String,
    content: @Composable (Modifier) -> Unit
) {
    val reactKey = PetLandmarks.reactCount(id)
    val poke = remember { Animatable(1f) }
    LaunchedEffect(reactKey) {
        if (reactKey > 0) {
            poke.snapTo(1f)
            poke.animateTo(1.05f, spring(dampingRatio = 0.45f, stiffness = 520f))
            poke.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f))
        }
    }
    DisposableEffect(screen, id) {
        onDispose {
            PetLandmarks.remove(screen, id)
            // v8.19 — also drop the poke counter: navigating back to this
            // screen composes at count 0, so no one-off pulse on arrival.
            PetLandmarks.resetReactCount(id)
            // v8.20 — clear any stale drag-hover highlight.
            PetLandmarks.setHovered(id, false)
        }
    }
    // v8.20 — the bed glows (sustained scale) while the pet is dragged
    // over it — the drop target reads before the drop.
    val hoverScale by animateFloatAsState(
        targetValue = if (PetLandmarks.isHovered(id)) 1.10f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 350f),
        label = "landmarkHover"
    )
    val landmarkModifier = Modifier
        .onGloballyPositioned { coords ->
            PetLandmarks.upsert(
                screen,
                PetLandmarks.Landmark(
                    id = id,
                    kind = kind,
                    bounds = Rect(
                        offset = coords.positionInWindow(),
                        size = Size(
                            coords.size.width.toFloat(),
                            coords.size.height.toFloat()
                        )
                    )
                )
            )
        }
        .graphicsLayer {
            scaleX = poke.value * hoverScale
            scaleY = poke.value * hoverScale
        }
    content(landmarkModifier)
}
