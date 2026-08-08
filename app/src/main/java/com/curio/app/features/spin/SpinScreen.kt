package com.curio.app.features.spin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioPassport
import com.curio.app.data.CurioPet
import com.curio.app.ui.pet.PetLandmark
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.data.CurioQuests
import com.curio.app.data.CurioRepositoryHolder
import com.curio.app.data.CurioTopic
import com.curio.app.data.ExploreSessionStore
import com.curio.app.data.SmartDensityMode
import com.curio.app.data.StreakTracker
import com.curio.app.data.TopicJsonLoader
import com.curio.app.navigation.CurioRoutes
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.ConfettiBurst
import com.curio.app.ui.components.CurioCategoryCard
import com.curio.app.ui.components.CurioNavTint
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioGradients
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioMixedDeck
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.categoryBackgroundWash
import com.curio.app.ui.theme.categoryBorder
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.categorySurface
import com.curio.app.ui.theme.fromHsl
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.theme.lightAccentTint
import com.curio.app.ui.theme.onAccent
import com.curio.app.ui.theme.pastelFillInk
import com.curio.app.ui.theme.themedAccent
import com.curio.app.ui.theme.toHsl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.curio.app.ui.adaptive.LocalRevealSharedScope
import com.curio.app.ui.adaptive.LocalRevealVisibilityScope
import com.curio.app.ui.adaptive.RevealBoundsTransform
import com.curio.app.ui.adaptive.RevealSharedElementKey
import com.curio.app.ui.components.MorphEntrance
import kotlin.random.Random

/**
 * The Spin — see Curio Spin contract.
 *
 * v5 changes:
 *  1. **Header chrome removed** — the Spin deck opens directly without a
 *     back button, category label, or topic-count badge.
 *  2. **Category picker moved to bottom** — "Categories" pill button in the
 *     bottom bar opens a beautiful tile-grid bottom sheet (like the Explore
 *     page) for switching categories.
 *  3. **Filter moved to bottom** — "Filter" pill button next to Categories.
 *  4. **Compact filter sheet** — redesigned with tighter spacing, toggle
 *     chips, and a clean apply button.
 *  5. **Unified bottom bar** — Categories · Filter · Shuffle all in
 *     one row for quick one-thumb access.
 *
 * v5.1 changes:
 *  6. **Fan-deck carousel** — redesigned shuffle cards: a tall paper
 *     "ticket" hero card (watermark glyph, subtype badge, name, tags,
 *     teaser, tap hint) with slim prev/next pill cards fanned above and
 *     below like a slot window.
 *  7. **Tap-to-open** — the landed card opens the topic directly (no
 *     Explore button); the bottom CTA becomes "Spin again".
 *
 * v5.2 changes:
 *  8. **Tap-open landing** — after the shuffle settles the landed card
 *     stays in place until the user taps it to open Topic Reveal. The
 *     Shuffle CTA owns all spin starts so accidental card taps never spin.
 *
 * v5.3 changes:
 *  9. **Saveable state** — active category, filter chips (tags + subtypes)
 *     and recent-topic history now persist via `rememberSaveable` across
 *     navigation (Spin → Reveal → back), rotation and process death.
 *     The landed topic stays transient on purpose so the deck opens cleanly
 *     after process restore.
 *
 * v5.4 changes:
 * 10. **Spec-timed spin window** — the shuffle duration is now randomized
 *     inside [CurioMotion.Durations.SpinMin]..[SpinMax] (2.4–3.2s) instead
 *     of a fixed loop, so every spin settles at a slightly different
 *     moment. The landed ticket swaps its helper copy while shuffling and
 *     then returns to the intentional "Tap to open" state.
 *
 * v5.5 changes:
 * 11. **Last-used category persists across launches** — the category the
 *     user spins in (chosen in-screen or opened via a category slug) is
 *     stored in [AppPreferences]; opening the plain Spin tab without a
 *     slug picks up where they left off instead of defaulting to Surprise.
 *
 * v5.6 changes:
 * 12. **Landed topic survives closing Reveal** — if the user closes Topic
 *     Reveal without saving, the topic stays on the card with "Tap to open"
 *     active until they explore (capture) it or tap Spin again.
 *
 * v5.7–v5.8 changes:
 * 13. The former gradient/glass ticket treatment was replaced by an opaque
 *     paper ticket with a category-color rule, crisp border, and layered
 *     elevation. The bottom controls use solid paper containers; no ambient
 *     halo or glossy surface treatment is used on this screen.
 *
 * v5.9–v5.10 changes:
 * 14. The optional Spin page feature toggles (roulette dial, ritual &
 *     anticipation, deck enrichment, screen furniture) were removed — the
 *     screen keeps the fan-deck carousel and simple spacing as its
 *     permanent design. In their place a muted watermark backdrop of all
 *     the category glyphs sits behind the content, so the quiet space
 *     around the deck carries a whisper of the Curio world.
 * 15. **Bigger dice button** — the center CTA grew to 118dp idle / 100dp
 *     landed (176dp container) with a larger dice glyph inside.
 * 16. **Dice in every state** — the Casino dice shows even in the "Spin
 *     again" state (previously a Refresh icon); accent-tinted on the
 *     neutral landed surface.
 * 17. **Fluid dice tumble** — the in-button dice animation was slowed from
 *     980ms to 1600ms per turn with LinearEasing (no restart snap) plus a
 *     breathing pulse on the orbiting pips.
 *
 * v6.3 changes:
 * 18. **Bigger deck + CTA** — the hero ticket grew to 286×310dp (carousel
 *     444dp) and the dice button to 126dp idle / 108dp landed, with the
 *     dice glyphs scaled up to match.
 *
 * v6.4 changes:
 * 19. **Peek cards catch up** — the slim background cards grew ~6%
 *     (318×102dp near, 288×84dp far) so the whole fan scales with the
 *     hero ticket instead of the peeks staying small behind the big card.
 *
 * v6.5 changes:
 * 20. **Peek cards grow again (~13%)** — the topic title inside each
 *     background card now has room to read instead of hiding behind the
 *     fan (360×116dp near, 328×96dp far; proportions kept, only size up).
 * 21. **Gentler hero bounce** — smaller per-tick kick (1.035), half the
 *     tilt (40° factor), a softer hop, and a lower landing rest scale so
 *     the shuffle pulses instead of slamming the card.
 *
 * v6.6 changes:
 * 22. **Calm reel cadence** — the spin window lengthens slightly
 *     (2.8–3.6s) and the tick interval glides from ~200ms to ~520ms on a
 *     plain sine ease instead of the old squared-sine whip (105→400ms),
 *     so the wheel reads as a graceful reel slowing down.
 * 23. **Hero content reels** — the ticket's title/tags/teaser now animate
 *     through an eased upward slide + fade on every tick (mimicking a
 *     background card rising to the front) instead of snapping instantly.
 * 24. **Softer tick pulse** — per-tick kick drops to 1.02 on a heavily
 *     damped low-stiffness spring, the rock halves to a 16° tilt, and the
 *     landing settle uses the controlled Deliberate spring (no Elastic
 *     bounce). Peek wipes switch from 90ms linear blurs to ~200ms
 *     FastOutSlowInEasing slides.
 *
 * v6.10 changes:
 * 25. **Coherent reel (no more glitchy start)** — the fan is dealt as a
 *     stable hand and the reel rotates through it (+1 per tick), with the
 *     idle fan and the spinning reel reading the SAME window. A spin now
 *     starts from the current spread as a seamless continuation; the old
 *     per-spin re-shuffle made all five cards jump to arbitrary topics in
 *     one frame. The hand re-deals around the landed topic when a spin
 *     settles (masked by the confetti).
 * 26. **Fluid peek wipes** — every slot now rises THROUGH the card window
 *     at full height (in from below, out the top), all in the same
 *     direction (the old top-peek inverted slide glided backwards), and
 *     the wipe duration sits UNDER the 200ms tick floor so each step
 *     completes before the next tick lands — a clean slot-reel instead of
 *     an interrupted blur.
 * 27. **Dice settles instead of stopping** — the tumbling dots morph into
 *     the resting dice (spring scale + fade, then a gentle idle breathe on
 *     the landed die) instead of hard-swapping, and the tumble gains a
 *     slow vertical bob so the loop reads as a die shaking — seamless,
 *     and it never just stops.
 *
 * v7.1 changes:
 * 28. **Directional peek wipes** — top peek cards now feed the deck from
 *     ABOVE (their content drops DOWN into the card) while bottom peeks
 *     rise up, so the fan streams toward the hero from both ends and a
 *     top card's title is never sliced off the top edge by the old
 *     upward wipe.
 * 29. **Soft glides, not hard cuts** — the full-height slot wipe (which
 *     sliced the title mid-slide and read as cut off) is replaced by a
 *     partial-height glide + fade at ~320ms (under the ~340ms tick floor),
 *     so each step completes before the next tick and the reel reads as
 *     calm and smooth instead of fast and glitchy.
 *
 * v7.7 changes (EXPERIMENTAL, four independent toggles):
 * 30. **Deck card redesign toggles** — Settings → Appearance → "Deck
 *     cards" (each OFF by default) swaps the flat peek-card slabs for the
 *     recommended deck treatment, one upgrade per toggle so each can be
 *     A/B'd alone: a top-lit two-stop gradient fill,
 *     a category-tinted hairline border (deep ink in light, light twin in
 *     dark), soft ambient shadows, and roomier near-card titles (16sp
 *     SemiBold, light tracking, two lines) with proportional glyphs (22dp
 *     near / 18dp far). The classic flat deck stays the shipping look
 *     until the experiment settles.
 * 31. **Pastel peek cards** — the peek fills now wear the pastel card
 *     family in pastel mode (airy pale layers in light, softly deepened
 *     muted twins in dark) instead of the old lerp-toward-black mid-tones.
 *
 * v7.9 changes:
 * 32. **Pastel hero ticket** — in pastel light mode a single-category
 *     deck's hero ticket opens on a pastel-family crown (a whisper of the
 *     pastel accent melting into the on-hue wash) instead of the
 *     black-darkened card fill, so the front card, the pale peek cards and
 *     the pastel spin button all read as one pastel story. Mixed decks
 *     already carried pure pastel stops; dark mode and non-pastel keep the
 *     classic card gradient.
 *
 * v7.8.1 changes:
 * 33. **Pastel brightness** — pastel card fills open on the full pastel
 *     accent (no black deepen) with a richer pastel saturation, so the
 *     shuffle main card no longer reads dimmed in either mode; pastel peek
 *     cards sit a step darker than the hero again (near 0.16 / far 0.28
 *     black-lerp in light) instead of glowing brighter than it.
 *
 * v7.17 changes:
 * 34. **Calm peeks in every palette** — the background peek cards behind
 *     the hero were too bright and vibrant in ALL modes. The fills now
 *     recede instead of glow: non-pastel peeks deepen (near 0.40 / far
 *     0.52 black-lerp, was 0.28/0.42) and desaturate (~0.80x, capped), and
 *     pastel peeks drop lightness harder (light 0.12/0.18, dark 0.11/0.16
 *     — was 0.06/0.10, 0.09/0.14) with a gentle saturation pull, so the
 *     airy pastels stay in family. The top-lit gradient crown softens to a
 *     whisper (0.04-0.06 white-lerp, was 0.10-0.14) — the gradient family
 *     stays, it just reads calm and quiet.
 *
 * v7.18 changes:
 * 35. **5% less saturated peeks** — the peek fills' saturation pull eases
 *     further (non-pastel 0.80x → 0.75x, pastel 0.85x → 0.80x) so the
 *     background cards read a touch calmer in every palette.
 */
// ════════��══════════════════════════════════════════════════════════════════
// Saveable-state savers — category persisted by enum name, filter sets as
// lists (Set<String> has no built-in Bundle saver).
// ═══════════════════════════════════════════════════════════════════════════

/** Serializes a List<CategoryId> (single or multi-category launch set) by enum name. */
private val CategoryIdListSaver = listSaver<List<CategoryId>, String>(
    save = { it.map { id -> id.name } },
    restore = { names -> names.mapNotNull { name -> CategoryId.values().firstOrNull { it.name == name } } }
)

/** Serializes a Set<String> (filter chips, recent ids) as a saveable list. */
private val StringSetSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() }
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpinScreen(categorySlug: String?, navController: NavController) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // v5.7.1 — the slug branch is CurioCategory?; the prefs fallback returns
    // a CategoryId, so resolve it through byId(...) to keep BOTH elvis
    // branches CurioCategory (mixing them inferred Any → MutableState<Any>
    // vs the saver's MutableState<CurioCategory> → CI compile failure).
    // v5.11 — multi-category launch: the category picker's Mix button can
    // pass a comma-joined slug list ("artists,albums"). Resolve each part; fall
    // back to the last-used single category when the slug is absent or
    // unresolvable.
    val initialCats = remember(categorySlug) {
        // v7.94 — hidden lanes (Manage Categories) must NEVER be dealt, even
        // when a slug or the last-used deck names them: filter through the
        // reactive visible set and fall back to Wildcard when everything is
        // hidden so the deck never opens empty.
        val visibleIds = CurioCategories.visible.map { it.id }.toSet()
        val resolved = categorySlug
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull { CurioCategories.byRouteSlug(it) }
            ?.filter { it.id in visibleIds }
            .orEmpty()
        val fromPrefs = AppPreferences.getLastSpinCategories(context)
            .map { CurioCategories.byId(it) }
            .filter { it.id in visibleIds }
        val cats = if (resolved.isNotEmpty()) resolved else fromPrefs
        if (cats.isNotEmpty()) cats
        else listOf(CurioCategories.byId(CategoryId.WILDCARD))
    }

    // v5.5 — remember which category this session opened in, so the plain
    // Spin tab opens where the user left off on the next launch. Persist the
    // FULL launch set (single or mixed) when a slug (single or multi) is
    // present so multi-select decks survive too.
    LaunchedEffect(Unit) {
        if (categorySlug != null) {
            AppPreferences.setLastSpinCategories(context, initialCats.map { it.id })
        }
    }

    // ── Saveable screen state — survives nav away/back, rotation and ──
    //    process death (v5.3). The active category SET persists across all
    //    of them; filters + recent history are keyed per first category so
    //    switching categories still resets them to fresh.
    var activeCatIds by rememberSaveable(
        initialCats.map { it.id },
        stateSaver = CategoryIdListSaver
    ) { mutableStateOf(initialCats.map { it.id }) }
    // v5.14 — a SLUG launch is authoritative. navigateToTab restores saved
    // state for the same route pattern, which could resurrect a stale
    // session (e.g. an in-screen category switch made inside an earlier
    // spin/artists visit) — picking "Artists" then reopened the deck with
    // Albums' pool. Whenever a slug is present, re-derive the category set
    // from it on arrival; user switches made AFTER arrival (picker sheet)
    // still win because this effect keys only on the slug.
    val slugCatIds: List<CategoryId>? = remember(categorySlug) {
        categorySlug
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull { CurioCategories.byRouteSlug(it)?.id }
            ?.takeIf { it.isNotEmpty() }
    }
    LaunchedEffect(categorySlug) {
        // Guard: on a normal fresh launch the value already matches; only
        // write when restoreState resurrected a stale set.
        if (slugCatIds != null && activeCatIds != slugCatIds) {
            activeCatIds = slugCatIds
        }
        // v5.15 — the plain Shuffle tab is equally authoritative from
        // prefs: the category picker ("What are we exploring?") now lands
        // here via navigateToTab(SPIN) after persisting its (possibly
        // mixed) selection, and restoreState could otherwise resurrect the
        // previous deck. Every in-screen switch also persists, so prefs
        // always reflect the user's latest deck.
        if (categorySlug == null) {
            val persisted = AppPreferences.getLastSpinCategories(context)
            if (persisted.isNotEmpty() && activeCatIds != persisted) {
                activeCatIds = persisted
            }
        }
    }
    // The first selected category drives the watermark accent and confetti
    // tint; the pool below merges every selected
    // category's topics so a multi-select launch spins across all of them.
    val activeCategory = remember(activeCatIds) {
        val id = activeCatIds.firstOrNull() ?: AppPreferences.getLastSpinCategory(context)
        CurioCategories.byId(id)
    }
    // Defensive: a corrupted saved state could restore an empty category
    // set — fall back to the last-used category so the pool still loads.
    val poolIds = if (activeCatIds.isEmpty()) listOf(AppPreferences.getLastSpinCategory(context)) else activeCatIds
    val pool by produceState<List<CurioTopic>>(initialValue = emptyList(), poolIds) {
        val merged = mutableListOf<CurioTopic>()
        val seen = mutableSetOf<String>()
        poolIds.forEach { id ->
            TopicJsonLoader.load(id).forEach { t -> if (seen.add(t.id)) merged.add(t) }
        }
        value = merged
    }

    // ── Multi-select filter state (per-category, saveable) ────────────
    var activeFilters by rememberSaveable(activeCategory.id, stateSaver = StringSetSaver) {
        mutableStateOf(setOf<String>())
    }
    var activeSubtypes by rememberSaveable(activeCategory.id, stateSaver = StringSetSaver) {
        mutableStateOf(setOf<String>())
    }
    var showFilters by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    // Broader OR-based filtering: a topic matches if it has ANY of the
    // selected tags AND its subtype is in the selected subtypes (or no
    // subtype filter is active).
    val filteredPool = remember(pool, activeFilters, activeSubtypes) {
        var r = pool
        if (activeFilters.isNotEmpty()) {
            r = r.filter { topic -> topic.tags.any { tag -> tag in activeFilters } }
        }
        if (activeSubtypes.isNotEmpty()) {
            r = r.filter { it.subtype in activeSubtypes }
        }
        r
    }
    // v7.80 — done topics (explored or marked "Already …") never appear in
    // the deck — not even as fan/peek cards. Falls back to the full
    // filtered pool when everything is done so the fan never empties.
    val deckPool = remember(filteredPool, ExploreSessionStore.doneTopicsState) {
        val open = filteredPool.filterNot { ExploreSessionStore.isDone(it.categoryId, it.name) }
        if (open.isNotEmpty()) open else filteredPool
    }
    // Smart filter groups — buckets raw tags into Type · Genre · Era ·
    // Origin sections and caps each, so the sheet stays ~10-15 chips
    // instead of dumping every raw tag (albums alone has 256 unique tags).
    val filterGroups = remember(pool) { buildFilterGroups(pool) }

    // ── Spin state ────────────────────────────────────────────────────
    var shuffling by remember { mutableStateOf(false) }
    var shuffleCount by remember { mutableIntStateOf(0) }
    var confettiTrigger by remember { mutableIntStateOf(0) }
    // ── Landed topic — persisted by NAME (v5.6) so closing Reveal without
    //    saving keeps it on the card, tappable, until explored or spun
    //    again. The full CurioTopic is re-derived from the pool below.
    var landedTopicName by rememberSaveable(activeCategory.id) {
        // Seeded from AppPreferences (v6): rememberSaveable survives tab
        // switches (saveState/restoreState) but dies when the Spin entry is
        // popped from the navigation stack. The prefs mirror
        // restores the landed card the next time Spin is composed.
        mutableStateOf(AppPreferences.getLandedTopic(context, activeCategory.id))
    }
    // v5.6 — true once THIS landing has already opened by tap; reset per spin.
    var landingAlreadyOpened by rememberSaveable(activeCategory.id) { mutableStateOf(false) }
    val landedTopic: CurioTopic? = remember(landedTopicName, filteredPool) {
        landedTopicName?.let { name ->
            filteredPool.firstOrNull { it.name == name }
                ?: TopicJsonLoader.cached(activeCategory.id)?.firstOrNull { it.name == name }
        }
    }
    // v6 — mirror the landed topic to AppPreferences whenever it changes
    // (landed on spin end, cleared on the next spin start), so it survives
    // ANY navigation — including popping the Spin back-stack entry.
    LaunchedEffect(activeCategory.id, landedTopicName) {
        AppPreferences.setLandedTopic(context, activeCategory.id, landedTopicName)
    }
    // True only during an explicit opening handoff; keeps copy flexible if
    // a future shared-element transition delays navigation.
    var isOpening by remember { mutableStateOf(false) }
    val openingScope = rememberCoroutineScope()
    var recentTopicIds by rememberSaveable(activeCategory.id, stateSaver = StringSetSaver) {
        mutableStateOf(setOf<String>())
    }

    // ── Spin hand — the 6-topic fan window (v6.10) ─────────────────────
    // A stable "hand" reels during a spin: it's dealt ONCE (a random
    // spread, centered on the current front topic) and the reel advances by
    // rotating cycleIndex through it — so every tick is a clean +1 shift
    // and the deck visibly streams past. The OLD per-spin re-shuffle made
    // the start of every spin glitch: all five cards swapped to arbitrary
    // topics in a single frame before the reel even began.
    // The initial deal centers on the RESTORED landed topic when one
    // exists (nav-return from Reveal), so the idle fan reads coherent even
    // after the back-stack drops the composition. Keyed on filteredPool
    // ONLY — not on deckPool and not on landedTopicName (v7.101): a topic
    // becoming DONE mid-session (explore → back) must never re-deal the
    // fan into a different spread — the deck stays exactly as it was until
    // the next spin, filter change, or category switch. The spin start
    // (which nulls the landed topic) also never re-deals the hand mid-flow.
    var hand by remember(filteredPool) { mutableStateOf(buildDeckHand(deckPool, landedTopic)) }
    // cycleIndex is NOT reset per spin — the reel starts from wherever the
    // deck stopped (the landed topic sits at hand[0]), so the first tick is
    // a seamless continuation instead of a jump cut.
    var cycleIndex by remember { mutableIntStateOf(0) }
    // Keep the reel position in sync whenever the hand is re-dealt by a
    // pool change (filters/category), so the fan always fronts hand[0].
    LaunchedEffect(filteredPool) { cycleIndex = 0 }
    val cat = activeCategory

    // ── Mixed-deck colors (v5.12) ───────────────────────────────────────
    // When several categories are selected, the deck wears a curated blend
    // of every chosen accent instead of the first category's color alone:
    // peek cards / spin button / confetti take the blended accent, and the
    // hero ticket takes a multi-accent gradient (Spotify-style).
    // Resolved in the composable body (NOT remember) so the Material style's
    // device-color blend of each accent updates when the theme style changes.
    val deckAccents = activeCatIds.map { CurioCategories.byId(it).themedAccent() }
    // v7.5 — pastel mode: the curated pair/triple blends are deep, so the
    // resolved deck accent softens to its theme-aware pastel twin (airy in
    // light, muted deep pastel in dark). `dark` is resolved here (the
    // remember block is not a @Composable context).
    val pastelMode = AppPreferences.pastelColorsState
    val darkMode = isCurioDarkTheme()
    val deckAccent = remember(deckAccents, pastelMode, darkMode) {
        CurioMixedDeck.mixedDeckAccent(deckAccents, pastel = pastelMode, dark = darkMode)
    }

    // ── Mixed-deck identity (v5.13) ───────────────────────────────────────
    // A multi-select deck presents as ONE "Mixed" category instead of
    // wearing the first selected category's name/glyph: sparkles glyph,
    // blended accent + tint, and the merged topic pool. The synthetic
    // deckCat is display-only — its id stays the first category's id, so
    // logic keys (landed topic, filters, reveal guard, last-used prefs)
    // keep operating on the real category set.
    val isMixedDeck = remember(activeCatIds) { activeCatIds.distinct().size > 1 }

    // v7.9 — pastel LIGHT mode: a single-category deck opens its hero
    // ticket on the pastel-family crown (a whisper of the pastel accent,
    // melting into the on-hue page wash) instead of categoryCardFill's
    // black-darkened start, so the front card wears the same pastel story
    // as the peek cards and the pastel spin button. v7.8.1 — the peeks
    // behind it now sit a step DEEPER (black-lerp near 0.16 / far 0.28)
    // so the hero's crown reads as the brightest card of the deck. Mixed
    // decks already carry pure pastel stops + pastel seams; dark mode and
    // non-pastel keep the classic card gradient (the muted pastel twins
    // already match the dark peeks).
    val deckGradient = if (pastelMode && !darkMode && !isMixedDeck) {
        // v7.12 — pastel crown depth: when the toggle is on, the top stop
        // gets a subtle 5% black deepen instead of the old 4% white lift,
        // so the card reads with a gentle darker crown for depth.
        val topCrown = if (AppPreferences.pastelCrownDepthState) {
            lerp(deckAccent, Color.Black, 0.05f)
        } else {
            lerp(deckAccent, Color.White, 0.04f)
        }
        listOf(
            topCrown,
            lightAccentTint(deckAccent, saturation = 0.22f, lightness = 0.80f)
        )
    } else {
        CurioMixedDeck.mixedDeckGradient(deckAccents)
    }
    val deckCat = remember(activeCatIds, deckAccent, activeCategory) {
        if (isMixedDeck) {
            activeCategory.copy(
                displayName = "Mixed",
                iconGlyph = CurioIcons.AutoAwesome,
                accent = deckAccent,
                // Pastel twin of the blend so categoryInk() stays readable on
                // dark surfaces (ink = lightAccent in dark mode).
                lightAccent = lerp(deckAccent, Color.White, 0.45f),
                tint = deckAccent.copy(alpha = 0.20f)
            )
        } else {
            activeCategory
        }
    }

    // ── Mixed-deck arrangement seed (v6.9) ───────────────────────────────
    // A deck's hero-gradient arrangement is keyed off its sorted category
    // ids, so different mixes get different non-linear treatments (diagonal
    // sweep / reversed diagonal / radial glow) while a given deck stays
    // stable. Single decks keep the plain vertical card gradient.
    val mixSeed = remember(activeCatIds) { activeCatIds.sorted().hashCode() }

    // Publish the page wash so the bottom nav bar (rendered by the NavHost
    // scaffold, outside this screen) can blend with the tinted Spin page. The
    // bar gates on its own route (spin prefix only), so publishing here never
    // tints Home or Cabinet. Keys on the resolved color so theme/dark-mode
    // and category changes republish automatically. A mixed deck wears THE
    // blended color the mix resolves to (mixedDeckWash) instead of the first
    // category's wash, so the page reads in the deck's mixed color story.
    val pageWash = if (isMixedDeck) CurioMixedDeck.mixedDeckWash(deckAccent)
                   else deckCat.categoryBackgroundWash()
    LaunchedEffect(pageWash) {
        CurioNavTint.publishSpinWash(pageWash)
    }
    // Keep the last published wash while the shared-element transition leaves
    // Spin. Clearing it here would make the Scaffold nav bar fall back to the
    // cream theme surface for one frame before the reveal placeholder takes
    // over, creating a visible color flash. Non-Spin routes ignore this handoff
    // and a new Spin composition republishes the current wash immediately.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // The preserved Spin tab resumes after Reveal closes. Clear
                // only the one-shot handoff state so the landed card returns
                // to its normal tappable resting presentation.
                isOpening = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Category switch resets transient animation state. The landed card is
    // deliberately NOT cleared here: landedTopicName is keyed by
    // activeCategory.id in rememberSaveable, so switching categories resets
    // it automatically — nulling it here would ALSO wipe the landed card on
    // every return from Topic Reveal (v5.6: stays tappable until spun again
    // or explored).
    LaunchedEffect(activeCategory.id) {
        shuffling = false
        isOpening = false
    }

    // ── Improved shuffle logic — sinusoidal ease-out deceleration ─────
    LaunchedEffect(shuffleCount) {
        if (shuffleCount == 0 || filteredPool.isEmpty()) return@LaunchedEffect
        shuffling = true
        // v8.13 — the pet cheers while the deck reels (cleared at the settle).
        CurioPet.noteSpinning(true)
        landedTopicName = null
        landingAlreadyOpened = false
        isOpening = false
        // v5.4 — randomized within the spec's spin window; every spin
        // settles at a slightly different moment like a real wheel.
        val durationMs = Random.nextLong(
            CurioMotion.Durations.SpinMin.toLong(),
            CurioMotion.Durations.SpinMax.toLong() + 1
        )
        val start = System.currentTimeMillis()
        var tick = 0
        while (true) {
            val elapsed = System.currentTimeMillis() - start
            if (elapsed >= durationMs) break
            val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
            // Smooth reel deceleration: a plain sine ease (no squaring) so
            // the wheel starts at a readable cadence and glides gently to a
            // stop — a graceful slow-down instead of a snappy whip. The
            // ~340ms floor keeps the fastest early ticks readable and sits
            // ABOVE the ~320ms peek wipe, so every transition completes
            // before the next tick lands. Intervals ~340ms -> ~520ms.
            val eased = sin(progress * Math.PI.toFloat() / 2f)
            val interval = (340L + (180L * eased).toLong()).coerceAtMost(520L)
            cycleIndex = ++tick
            // Slot-machine ratchet: haptic intensity escalates as the wheel
            // decelerates — a light tick at the brisk opening cadence, a
            // firmer segment tick through the slowdown, and a solid
            // keyboard-tap click in the final settle phase. As intervals
            // lengthen, ticks naturally space out like a prize wheel
            // locking in. NOTE: SegmentFrequentTick / KeyboardTap are
            // the renamed equivalents of the old ClockTick / Keypress
            // constants (Compose UI 1.12) — do NOT revert them.
            val ratchet = when {
                progress < 0.5f -> HapticFeedbackType.TextHandleMove
                progress < 0.85f -> HapticFeedbackType.SegmentFrequentTick
                else -> HapticFeedbackType.KeyboardTap
            }
            haptics.performHapticFeedback(ratchet)
            delay(interval)
            if (System.currentTimeMillis() - start >= durationMs) break
        }
        shuffling = false
        // v8.13 — the reel stopped; the pet settles back to watching.
        CurioPet.noteSpinning(false)

        // Pick a single topic — tier-biased, sentiment-weighted (liked /
        // disliked topics + category affinity), and never an already-
        // explored or done topic while alternatives remain. "Done" topics
        // (explored, or marked "Already watched / listened / read / explored"
        // on the reveal screen) are excluded exactly like captured ones —
        // v7.80. pickFrom still falls back to the full pool when everything
        // is exhausted, so the shuffle never runs dry.
        val exploredIds = runCatching {
            CurioRepositoryHolder.repo.getAll().map { it.topic.id }.toSet()
        }.getOrDefault(emptySet())
        val doneIds = filteredPool
            .filter { ExploreSessionStore.isDone(it.categoryId, it.name) }
            .map { it.id }
            .toSet()
        val primary = pickFrom(
            filteredPool,
            recentTopicIds,
            exploredIds + doneIds,
            AppPreferences.topicSentimentsState,
            AppPreferences.categoryAffinityMap()
        )
        landedTopicName = primary?.name
        if (primary != null) {
            // Re-deal the hand around the landed topic — the front becomes
            // the pick and its neighbors fill the fan — so the deck stops
            // on a coherent spread and the NEXT spin starts from it
            // seamlessly. This re-deal is masked by the confetti burst.
            hand = buildDeckHand(deckPool, primary)
            cycleIndex = 0
            recentTopicIds = (recentTopicIds + primary.id).toList().takeLast(20).toSet()
            StreakTracker.recordActivity(context)
            // Feed the quests system — spins drive journey + daily + badges.
            // The spun lane is passed so a "New lane" discovery daily aimed
            // at this category (or Wildcard's surprise deck) completes at the
            // spin, no topic-open required (spec §6.3).
            CurioQuests.onSpin(context, activeCategory.id)
            // Feed the category passport — the spin counts toward the lane's
            // stamp and drives discovery quests (spec §6).
            CurioPassport.noteSpin(context, activeCategory.id)
            // The pet cheers when the wheel lands (spec §10.6 event hook).
            CurioPet.reactTo(CurioPet.Event.SPIN_LANDED)
            // Final reel clunk — strong confirmation the wheel locked in.
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        confettiTrigger++

        // Auto-open the landed topic: once the wheel settles, reveal it
        // immediately. The landed card state is preserved (landedTopicName +
        // landingAlreadyOpened), so returning from Reveal keeps it tappable
        // until spun again — nothing else about the flow changes. A short
        // pause lets the settle + confetti read before navigating; spinning
        // again within that window cancels this effect (keyed on
        // shuffleCount) and no navigation happens.
        if (primary != null) {
            // v8.16 — auto-open is a user preference (Settings → Appearance,
            // "Auto-open landed topic"), DEFAULT OFF: when disabled, the
            // deck just lands and the front card stays tappable — no reveal
            // page, no open-it prompt — until the user taps the card.
            if (AppPreferences.autoOpenRevealState) {
                // Keep the settled ticket visible as the source of the
                // handoff: its restrained lift/scale and "Opening…" label
                // should be on screen before the destination hero appears.
                landingAlreadyOpened = true
                isOpening = true
                // v7.x — shortened from 600ms: the settle + confetti still
                // get their beat, but the reveal arrives before the pause
                // reads as a stall.
                delay(450)
                // Guard against a category switch during the pause: the
                // effect captured `cat` at launch, so only navigate if it's
                // still the active category.
                if (cat.id != activeCategory.id) {
                    isOpening = false
                    return@LaunchedEffect
                }
                navController.navigate(CurioRoutes.revealFor(primary.categoryId.routeSlug, primary.name)) {
                    launchSingleTop = true
                }
            }
        }
    }

    // ── Landed topic auto-opens on landing ───────────────────────────
    // The wheel now reveals its landed topic automatically; the center card
    // is no longer a spin trigger — it opens an already landed topic, while
    // the Shuffle CTA owns all spin/shuffle starts.

    // ── v5.9 — landed card stays tappable until the user explicitly
    //    spins/shuffles again.  No longer auto-clears when explored.

    // ── Animations ────────────────────────────────────────────────────
    val buttonPulse by animateFloatAsState(
        targetValue = if (shuffling) 1.06f else 1f,
        animationSpec = CurioMotion.Springs.Snappy,
        label = "buttonPulse"
    )

    // ── Deck interaction callbacks — shared by the normal and compact
    //    layout branches (the Carousel call lives in SpinDeckSection) ─
    val onDeckCardTap: () -> Unit = {
        if (!shuffling && filteredPool.isNotEmpty() && !isOpening) {
            // v7.106 — the front card is ALWAYS openable: the restored
            // landed topic wins when present, otherwise whatever topic the
            // fan is showing right now (idle deck included — no shuffle
            // required). resolveTopicForSlot is the EXACT same resolution
            // the carousel uses to draw the front card, so tapping always
            // opens the topic the card is actually displaying.
            val resolved = resolveTopicForSlot(0, hand, cycleIndex, landedTopic)
            if (resolved != null) {
                landingAlreadyOpened = true
                isOpening = true
                // Give the settled ticket time to grow before the reveal
                // destination enters. This mirrors the automatic landing
                // handoff instead of making a manual tap feel like a cut.
                openingScope.launch {
                    // v7.x — the morph IS the expansion; this brief pause
                    // (100ms) lets the "Opening…" label read without adding
                    // perceptible lag. No pre-grow scale — the shared
                    // element handles the expansion from the exact card
                    // position.
                    delay(100)
                    if (isOpening) {
                        navController.navigate(
                            CurioRoutes.revealFor(resolved.categoryId.routeSlug, resolved.name)
                        ) {
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }
    val onSpinClick: () -> Unit = {
        if (!shuffling && filteredPool.isNotEmpty()) shuffleCount++
    }

    // ── Overall layout ─────────────────────────────────────────────────
    // Paper surfaces sit directly on the quiet theme background. All depth
    // comes from opaque cards, crisp rules, and elevation—not ambient washes.
    // v6.11 — BoxWithConstraints measures the height this screen is actually
    // granted (see [SpinCompactThresholdHeight]): short screens switch to
    // the compact layout below, normal screens keep this exact layout.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Category tint wash — the Spin page wears a wash of the deck's
            // color over the theme background (same wash language as Topic
            // Reveal / Save / Cabinet). Mixed decks wear THE blended color
            // the mix resolves to (pageWash) at high strength, so each mix
            // visibly repaints the page in its own blended color story.
            .background(pageWash)
    ) {
        // ── Smart layout tiers (v7.3) ────────────────────────────────
        // 1. DENSITY (three-way, toggleable via Settings → Smart density)
        //    — Compact responds to device density, while 2x explicitly
        //    forces the smaller deck on every device. High-density screens
        //    get a roomier deck only in Compact mode. The whole rule is
        //    gated by the picker.
        // 2. DIMENSION (toggleable via Settings → Smart Spin layout) —
        //    heights under [SpinCompactThresholdHeight] switch to the
        //    compact layout, and heights under
        //    [SpinExtraCompactThresholdHeight] get the EXTRA-compact tier —
        //    a smaller deck AND Categories/Filter as tall vertical pills
        //    pinned to the left/right screen edges.
        val densityMode = AppPreferences.smartDensityModeState
        val densityDpi = context.resources.displayMetrics.densityDpi
        val densityActive = densityMode != SmartDensityMode.OFF
        val lowDensity = densityActive && densityDpi < SpinLowDensityDpi
        val highDensity = densityActive && densityDpi >= SpinHighDensityDpi
        // v7.4 — the 2x tier: selecting EXTRA_COMPACT is itself the
        // explicit request for the smallest deck. It must not be gated by
        // the device's physical dpi; a high-density phone should also become
        // smaller when the user chooses 2x.
        val densityExtraCompact = densityMode == SmartDensityMode.EXTRA_COMPACT
        val smartLayout = AppPreferences.smartSpinLayoutState
        val heightCompact = maxHeight < SpinCompactThresholdHeight
        val extraCompact = smartLayout && maxHeight < SpinExtraCompactThresholdHeight
        // Extra-compact implies heightCompact (600 < 680), so this stays
        // true whenever the smaller tier is active.
        val compactHeight = densityExtraCompact || lowDensity || (smartLayout && heightCompact)
        // Roomy tier — high-density screens get a slightly LARGER deck so
        // the density rule works both ways. Keyed off the RAW height (not
        // the toggle-gated compactHeight) so a short high-density screen
        // never gets the bigger deck even when the dimension rule is off.
        val roomy = highDensity && !heightCompact && !densityExtraCompact
        // ── v7.15 — Continuous fit scale ──────────────────────────────
        // The deck now compresses to the space ACTUALLY available — the
        // height left between the deck and bottom controls, AND the screen
        // width — so on small screens the whole deck shrinks together with
        // the Category/Filter pills instead of overflowing while the
        // controls squeeze. The tier scales (compact / extra-compact /
        // roomy) still apply on top; the fit scale just guarantees the deck
        // never exceeds its space. With the header removed, only the bottom
        // controls and the deck's own breathing spacer are reserved here.
        val bottomBarEst = if (extraCompact) 108.dp else 80.dp
        val fitHeight = (maxHeight - bottomBarEst).coerceAtLeast(220.dp)
        val (fitChrome, fitCarousel) = when {
            extraCompact -> 144.dp to 350.dp
            compactHeight -> 158.dp to 390.dp
            else -> 222.dp to 444.dp
        }
        val heightFit = ((fitHeight - fitChrome) / fitCarousel).coerceIn(0.58f, 1f)
        // The near peek cards are 360dp wide at full scale; keep ~12dp of
        // page margin on each side so the fan never runs off the screen.
        val widthFit = ((maxWidth - 24.dp) / 360.dp).coerceIn(0.64f, 1f)
        val fitScale = minOf(heightFit, widthFit)
        // v7.x — the wide/landscape stage scales by the width left for the
        // deck (minus ~130dp side rail). Computed HERE in the
        // BoxWithConstraints scope rather than inside the Row/Column below:
        // the nested layout lambdas can't resolve this scope's maxWidth as
        // an implicit receiver, which broke the CI build.
        val wideFit = ((maxWidth - 130.dp) / 360.dp).coerceIn(
            if (compactHeight) 0.62f else 0.78f, 1f
        )
        // ── Watermark backdrop — every category glyph scattered around ──
        //    the screen in a muted shade, behind all content, so the quiet
        //    space around the deck still carries a whisper of the Curio
        //    world. The active category's glyph gets a faint accent tint.
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(activeCat = deckCat)
        }

        // ── Landscape / tablet: side-by-side layout (v7.x) ─────────
        //    On wide windows the bottom bar would waste horizontal space;
        //    Categories + Filter move to a right-edge rail as tall
        //    vertical pills, and the deck + Spin button stay centered.
        val wide = windowWidthSizeClass().isWide
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Center stage — deck carousel + Spin button, sized to the
                // available width minus the side rail.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SpinDeckSection(
                        compact = compactHeight,
                        extraCompact = false,
                        densityExtraCompact = false,
                        roomy = false,
                        cat = deckCat,
                        deckAccent = deckAccent,
                        deckGradient = deckGradient,
                        isMixed = isMixedDeck,
                        mixSeed = mixSeed,
                        displayPool = hand,
                        cycleIndex = cycleIndex,
                        shuffling = shuffling,
                        landedTopic = landedTopic,
                        opening = isOpening,
                        enabled = filteredPool.isNotEmpty() && !shuffling,
                        buttonPulse = buttonPulse,
                        fitScale = wideFit,
                        onCardTap = onDeckCardTap,
                        onSpinClick = onSpinClick
                    )
                }
                // Right rail — Categories + Filter as tall vertical pills
                Column(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    VerticalDeckButton(
                        label = if (isMixedDeck) "Mixed · ${activeCatIds.distinct().size}" else deckCat.displayName,
                        icon = deckCat.iconGlyph,
                        cat = deckCat,
                        selected = true,
                        onClick = { showCategoryPicker = true },
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    VerticalDeckButton(
                        label = if (activeFilters.isNotEmpty() || activeSubtypes.isNotEmpty())
                            "Filter · ${activeFilters.size + activeSubtypes.size}" else "Filter",
                        icon = CurioIcons.Search,
                        cat = deckCat,
                        selected = activeFilters.isNotEmpty() || activeSubtypes.isNotEmpty(),
                        onClick = { showFilters = true },
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        } else {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        if (compactHeight) {
            // ── Compact layout (small screens) ────────────────────────
            // The deck + spin button scroll inside the space above the
            // pinned Categories/Filter bar, so the
            // controls are never pushed off-screen; sizes step down via
            // SpinDeckSection(compact = true).
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                // Keep the deck, its peek cards, and the Spin button centered
                // horizontally and anchored just above Categories/Filter. A
                // centered vertical arrangement was creating a large empty
                // pocket between the button and the bottom controls.
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                SpinDeckSection(
                    compact = true,
                    extraCompact = extraCompact,
                    densityExtraCompact = densityExtraCompact,
                    roomy = false,
                    cat = deckCat,
                    deckAccent = deckAccent,
                    deckGradient = deckGradient,
                    isMixed = isMixedDeck,
                    mixSeed = mixSeed,
                    displayPool = hand,
                    cycleIndex = cycleIndex,
                    shuffling = shuffling,
                    landedTopic = landedTopic,
                    opening = isOpening,
                    enabled = filteredPool.isNotEmpty() && !shuffling,
                    buttonPulse = buttonPulse,
                    fitScale = fitScale,
                    onCardTap = onDeckCardTap,
                    onSpinClick = onSpinClick
                )
            }
        } else {
            // ── Normal layout — center the complete deck stage in the
            //    available space, then keep Categories/Filter directly below
            //    it. The old weighted spacer lived AFTER the Spin button and
            //    created the oversized gap the user saw above the filters.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                // The stage owns the remaining height, but its content should
                // sit at the bottom of that stage so the Spin button stays
                // close to Categories/Filter instead of floating midway above
                // an unused gap.
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SpinDeckSection(
                        compact = false,
                        densityExtraCompact = densityExtraCompact,
                        roomy = roomy,
                        cat = deckCat,
                        deckAccent = deckAccent,
                        deckGradient = deckGradient,
                        isMixed = isMixedDeck,
                        mixSeed = mixSeed,
                        displayPool = hand,
                        cycleIndex = cycleIndex,
                        shuffling = shuffling,
                        landedTopic = landedTopic,
                        opening = isOpening,
                        enabled = filteredPool.isNotEmpty() && !shuffling,
                        buttonPulse = buttonPulse,
                        fitScale = fitScale,
                        onCardTap = onDeckCardTap,
                        onSpinClick = onSpinClick
                    )
                }
            }
        }

        // ── 5. Bottom bar — Categories · Filter (controls only) ────
        // No duplicate shuffle button: the big center SpinButton above
        // owns all spin starts, so the bottom bar is controls only. On the
        // extra-compact tier the two pills move to the left/right screen
        // edges and stand vertically, so the middle stays clear.
        BottomCta(
            cat = deckCat,
            mixedCount = activeCatIds.distinct().size,
            filterActiveCount = activeFilters.size + activeSubtypes.size,
            vertical = extraCompact,
            onCategories = { showCategoryPicker = true },
            onFilter = { showFilters = true }
        )
        }
        }
    }



    // ── CategoryPickerSheet ───────────────────────────────────────────
    if (showCategoryPicker) {
        CategoryPickerSheet(
            currentCat = deckCat,
            onDismiss = { showCategoryPicker = false },
            onCategorySelected = { c ->
                activeCatIds = listOf(c.id)
                // v5.5 — persist so the Spin tab reopens on this category
                // after the app is killed and relaunched.
                AppPreferences.setLastSpinCategories(context, listOf(c.id))
                showCategoryPicker = false
            },
            onCategoriesSelected = { cats ->
                activeCatIds = cats.map { it.id }
                // v5.15 — persist the FULL mixed set (not just the first)
                // so a multi-select deck survives back navigation, tab
                // switches and app restarts.
                AppPreferences.setLastSpinCategories(context, cats.map { it.id })
                showCategoryPicker = false
            },
            onBrowseAll = {
                showCategoryPicker = false
                navController.navigate(CurioRoutes.PICKER) { launchSingleTop = true }
            }
        )
    }

    // ── ModalBottomSheet — compact multi-select filter dialog ──────────
    if (showFilters) {
        FilterSheet(
            cat = deckCat,
            groups = filterGroups,
            initialSubtypes = activeSubtypes,
            initialFilters = activeFilters,
            onDismiss = { showFilters = false },
            onApply = { tags, subtypes ->
                activeFilters = tags
                activeSubtypes = subtypes
                showFilters = false
            }
        )
    }

    // ── Confetti on landing ────────────────────────────────────────────
    if (confettiTrigger > 0) {
        ConfettiBurst(
            colors = listOf(deckAccent, deckCat.tint, CurioColors.ButterYellow),
            trigger = confettiTrigger,
            particleCount = CurioMotion.ConfettiParticleCountLarge,
            modifier = Modifier.fillMaxSize(),
            onComplete = {}
        )
    }
}

/**
 * The deck section of the Spin layout — breathing spacer, card carousel and
 * center spin button — shared by the normal and compact layout branches.
 *
 * [compact] switches between the two size variants: normal keeps the exact
 * pre-v6.11 measurements (44dp spacer, 444dp carousel, 32/20dp button
 * padding); compact tightens them (20dp spacer, 390dp carousel, 16/10dp
 * padding) and scales the deck itself down via the carousel's compact flag.
 */
@Composable
private fun ColumnScope.SpinDeckSection(
    compact: Boolean,
    extraCompact: Boolean = false,
    densityExtraCompact: Boolean = false,
    roomy: Boolean = false,
    cat: CurioCategory,
    deckAccent: Color,
    deckGradient: List<Color>,
    isMixed: Boolean,
    mixSeed: Int,
    displayPool: List<CurioTopic>,
    cycleIndex: Int,
    shuffling: Boolean,
    landedTopic: CurioTopic?,
    opening: Boolean,
    enabled: Boolean,
    buttonPulse: Float,
    fitScale: Float = 1f,
    onCardTap: () -> Unit,
    onSpinClick: () -> Unit
) {
    // ── Breathing room before the deck (tighter when the screen is short;
    //    roomier on high-density screens so the bigger deck has space) ────
    Spacer(
        Modifier.height(
            when {
                densityExtraCompact -> 12.dp
                extraCompact -> 12.dp
                compact -> 20.dp
                roomy -> 56.dp
                else -> 44.dp
            }
        )
    )

    // ── Carousel (interactive cards) — fitScale compresses the fan to the
    //    space actually available (see SpinScreen's fit-scale computation).
    Carousel(
        cat = cat,
        deckAccent = deckAccent,
        deckGradient = deckGradient,
        isMixed = isMixed,
        mixSeed = mixSeed,
        displayPool = displayPool,
        cycleIndex = cycleIndex,
        shuffling = shuffling,
        landedTopic = landedTopic,
        opening = opening,
        enabled = enabled,
        compact = compact,
        extraCompact = extraCompact,
        densityExtraCompact = densityExtraCompact,
        roomy = roomy,
        fitScale = fitScale,
        onCardTap = onCardTap,
        modifier = Modifier.fillMaxWidth()
    )

    // ── Center spin button — the ONLY shuffle CTA (v6) ──────────────
    // v8.16 — the spin button is a FUN pet landmark: the pet sometimes
    // dashes over and boops it while the deck waits (it just pulses — no
    // layout change, and the shared-element morph is untouched).
    PetLandmark(
        id = "spin",
        kind = PetLandmarks.Kind.FUN,
        screen = "spin"
    ) { m ->
        Box(
            modifier = m
                .fillMaxWidth()
                .padding(
                    top = when {
                        densityExtraCompact -> 12.dp
                        compact && extraCompact -> 12.dp
                        compact -> 16.dp
                        roomy -> 40.dp
                        else -> 32.dp
                    },
                    bottom = when {
                        densityExtraCompact -> 8.dp
                        compact && extraCompact -> 8.dp
                        compact -> 10.dp
                        roomy -> 18.dp
                        else -> 12.dp
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            SpinButton(
                tint = deckAccent,
                isShuffling = shuffling,
                landedTopic = landedTopic,
                pulseScale = buttonPulse,
                enabled = enabled,
                compact = compact,
                fitScale = fitScale,
                onClick = onSpinClick
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ═══════════════════════════════════════════════════════════════════════════
// Smart filter grouping — buckets a category's raw tags into compact
// Type · Genre · Era · Origin sections so the sheet stays ~10-15 chips
// instead of dumping every raw tag (albums alone has 256 unique tags).
// ═══════════════════════════════════════════════════════════════════════════

private data class FilterGroups(
    val types: List<String>,
    val genres: List<String>,
    val eras: List<String>,
    val origins: List<String>
)

/** Common nationality/origin tags — anything else is treated as a genre. */
private val NationalityTags = setOf(
    "American", "British", "French", "German", "Italian", "Japanese", "Chinese",
    "Nigerian", "Jamaican", "Canadian", "Swedish", "Norwegian", "Danish", "Finnish",
    "Icelandic", "Cuban", "Brazilian", "Indian", "Korean", "Australian", "Irish",
    "Scottish", "Welsh", "Russian", "Polish", "Spanish", "Portuguese", "Greek",
    "Turkish", "Mexican", "Argentine", "Argentinian", "Colombian", "Chilean", "Dutch",
    "Belgian", "Swiss", "Austrian", "Hungarian", "Czech", "Romanian", "Ukrainian",
    "Ghanaian", "Senegalese", "Ethiopian", "Kenyan", "South African", "Egyptian",
    "Moroccan", "Algerian", "Iranian", "Israeli", "Pakistani", "Filipino", "Indonesian",
    "Thai", "Vietnamese", "Malaysian", "Congolese", "Malian", "Lebanese", "Syrian",
    "Iraqi", "Afghan", "Armenian", "Georgian", "Kazakh", "Mongolian", "Nepali",
    "Sri Lankan", "Bangladeshi", "Haitian", "Puerto Rican", "Dominican", "Venezuelan",
    "Ecuadorian", "Bolivian", "Uruguayan", "Croatian", "Serbian", "Bulgarian", "Slovak",
    "Estonian", "Lithuanian", "New Zealand", "New Zealander", "Taiwanese", "Hong Kong",
    "Cape Verdean", "Barbadian", "Beninese", "African", "European", "Soviet", "Tuareg",
    // Film/TV industry regions — Hollywood (US studio system) and Bollywood
    // (Hindi cinema) read as origin tags on Films/Directors, so the Origin
    // bucket offers them alongside British / French / Korean / Indian…
    "Hollywood", "Bollywood",
    "Congolese", "Panamanian", "Chilean", "Argentine", "Puerto Rican",
    "American-British", "British-Nigerian", "American-Canadian", "French-Algerian",
    "Italian-American", "British-Irish", "African-American", "British-Canadian",
    "Brazilian-American", "Brazilian-British", "Ghanaian-British", "French-Spanish",
    "Irish-British", "British-American", "Canadian-American", "Greek-American",
    "Russian-French", "British-German", "Czech-Austrian", "Latvian-American",
    "French-American", "Swiss-American", "Japanese-American", "Hellenistic-Egyptian",
    "Roman-Egyptian", "Egyptian-Greek", "Polish-French", "New Zealand-British",
    "Welsh-British", "Scottish-British", "Hungarian-American", "British-Dutch",
    "American-French", "Austrian-Czech", "British-Welsh", "Indian-Bengali"
)

/**
 * Derives compact, meaningful filter chips from a category's pool.
 * Eras are the most frequent decades/centuries present, genres and origins
 * are the most-used tags, each capped so the sheet stays tidy.
 */
private fun buildFilterGroups(pool: List<CurioTopic>): FilterGroups {
    if (pool.isEmpty()) return FilterGroups(emptyList(), emptyList(), emptyList(), emptyList())
    val types = pool.map { it.subtype }.distinct().sorted()
    val counts = pool.flatMap { it.tags }.groupingBy { it }.eachCount()
    // Era chips: pick whichever family is more prevalent in this category —
    // decades (1970s…) for music/film, centuries (20th Century…) for books,
    // science and art. Comparing total frequency instead of mere presence
    // keeps the row coherent when a category mixes both (e.g. books has a
    // lone '2000s' tag but is dominated by '20th Century').
    val decadeRe = Regex("""\d{4}s""")
    val centuryRe = Regex("""^\d{1,2}(st|nd|rd|th) Century$|^Ancient$""")
    val decades = counts.keys.filter { decadeRe.matches(it) }
    val centuries = counts.keys.filter { centuryRe.matches(it) }
    val decadesTotal = decades.sumOf { counts[it] ?: 0 }
    val centuriesTotal = centuries.sumOf { counts[it] ?: 0 }
    val eras = (if (decadesTotal >= centuriesTotal) decades else centuries)
        .sortedByDescending { counts[it] ?: 0 }
        .take(4)
        .sorted()
    val origins = counts.keys
        .filter { it in NationalityTags }
        .sortedByDescending { counts[it] ?: 0 }
        .take(3)
    val genres = counts.keys
        .filter { !decadeRe.matches(it) && !centuryRe.matches(it) && it !in NationalityTags }
        .sortedByDescending { counts[it] ?: 0 }
        .take(4)
    return FilterGroups(types = types, genres = genres, eras = eras, origins = origins)
}

// ═══════════════════════════════════════════════════════════════════════════
// ═══════════════════════════════════════════════════════════════════════════
// Compact filter bottom sheet with visible selected-filter chips
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    cat: CurioCategory,
    groups: FilterGroups,
    initialSubtypes: Set<String>,
    initialFilters: Set<String>,
    onDismiss: () -> Unit,
    onApply: (tags: Set<String>, subtypes: Set<String>) -> Unit
) {
    val subtypes = groups.types
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftFilters by remember(initialFilters) { mutableStateOf(initialFilters) }
    var draftSubtypes by remember(initialSubtypes) { mutableStateOf(initialSubtypes) }
    val activeCount = draftFilters.size + draftSubtypes.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            // ── Header row ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurioIcon(cat.iconGlyph, null, tint = cat.categoryInk(), size = 22.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = cat.displayName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (activeCount > 0) {
                    TextButton(
                        onClick = {
                            draftFilters = emptySet()
                            draftSubtypes = emptySet()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Clear all",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ── Active filter summary chips — this is what was missing ─
            if (activeCount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Active filters",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        draftSubtypes.forEach { st ->
                            ActiveFilterChip(
                                label = st,
                                accent = cat.themedAccent(),
                                ink = cat.onAccent(),
                                onRemove = { draftSubtypes = draftSubtypes - st }
                            )
                        }
                        draftFilters.forEach { tag ->
                            ActiveFilterChip(
                                label = tag,
                                accent = cat.themedAccent(),
                                ink = cat.onAccent(),
                                onRemove = { draftFilters = draftFilters - tag }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            val hasAny = subtypes.size > 1 ||
                groups.genres.isNotEmpty() ||
                groups.eras.isNotEmpty() ||
                groups.origins.isNotEmpty()
            if (!hasAny) {
                Text(
                    text = "No filters for this category yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            } else {
                // ── Divider line ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )

                // ── Compact lazy chip grid — grouped Type · Genre · Era ·
                //    Origin sections, each capped to a handful of chips so
                //    the sheet stays tidy instead of 100+ raw tags. ─────
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 112.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (subtypes.size > 1) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionLabel("Type", Modifier.padding(bottom = 2.dp))
                        }
                        items(subtypes) { st ->
                            CompactChip(
                                label = st,
                                selected = st in draftSubtypes,
                                accent = cat.themedAccent(),
                                ink = cat.onAccent(),
                                chipSurface = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh),
                                chipBorder = cat.categoryBorder(
                                    fallback = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ),
                                onClick = {
                                    draftSubtypes = if (st in draftSubtypes) draftSubtypes - st else draftSubtypes + st
                                }
                            )
                        }
                    }
                    if (groups.genres.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionLabel(
                                "Genres",
                                Modifier.padding(top = if (subtypes.size > 1) 6.dp else 0.dp, bottom = 2.dp)
                            )
                        }
                        items(groups.genres) { tag ->
                            CompactChip(
                                label = tag,
                                selected = tag in draftFilters,
                                accent = cat.themedAccent(),
                                ink = cat.onAccent(),
                                chipSurface = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh),
                                chipBorder = cat.categoryBorder(
                                    fallback = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ),
                                onClick = {
                                    draftFilters = if (tag in draftFilters) draftFilters - tag else draftFilters + tag
                                }
                            )
                        }
                    }
                    if (groups.eras.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionLabel("Era", Modifier.padding(top = 6.dp, bottom = 2.dp))
                        }
                        items(groups.eras) { era ->
                            CompactChip(
                                label = era,
                                selected = era in draftFilters,
                                accent = cat.themedAccent(),
                                ink = cat.onAccent(),
                                chipSurface = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh),
                                chipBorder = cat.categoryBorder(
                                    fallback = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ),
                                onClick = {
                                    draftFilters = if (era in draftFilters) draftFilters - era else draftFilters + era
                                }
                            )
                        }
                    }
                    if (groups.origins.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionLabel("Origin", Modifier.padding(top = 6.dp, bottom = 2.dp))
                        }
                        items(groups.origins) { origin ->
                            CompactChip(
                                label = origin,
                                selected = origin in draftFilters,
                                accent = cat.themedAccent(),
                                ink = cat.onAccent(),
                                chipSurface = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh),
                                chipBorder = cat.categoryBorder(
                                    fallback = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ),
                                onClick = {
                                    draftFilters = if (origin in draftFilters) draftFilters - origin else draftFilters + origin
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Apply button ──────────────────────────────────────────
            Button(
                onClick = { onApply(draftFilters, draftSubtypes) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cat.themedAccent(),
                    contentColor = cat.onAccent()
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                CurioIcon(CurioIcons.Check, null, tint = cat.onAccent(), size = 18.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (activeCount > 0) "Apply filters ($activeCount)" else "Show all topics",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
    }
}

/** Chip showing an active filter with ✕ to remove. */
@Composable
private fun ActiveFilterChip(
    label: String,
    accent: Color,
    ink: Color = Color.White,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = accent,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = ink
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.clickable(onClick = onRemove)
            ) {
                CurioIcon(
                    CurioIcons.Close, null,
                    tint = CurioColors.DeepPlum,
                    size = 14.dp,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun CompactChip(
    label: String,
    selected: Boolean,
    accent: Color,
    ink: Color = Color.White,
    chipSurface: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    chipBorder: BorderStroke? = null,
    onClick: () -> Unit
) {
    // Plain Surface + clickable (no M3 minimum touch-target inflation) keeps
    // the chips compact even with 100+ tags in the sheet.
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) accent else chipSurface,
        border = if (selected) null else chipBorder,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                ),
                color = if (selected) ink else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
}


// ═══════════════════════════════════════════════════════════════════════════
// Fan-deck carousel — hero "ticket" card + slim prev/next peek cards
// ═══════════════════════════════════════════════════════════════════════════

/** Rest scale the hero card settles to after a shuffle lands. */
private const val LandedRestScale = 1.02f

// v7.1 — peek wipe timings. Soft partial-height glides + fades (no hard
// slot cut), all under the ~340ms tick floor so each step completes before
// the next tick lands.
private const val PeekWipeInMs = 320
private const val PeekWipeOutMs = 300
private const val PeekIdleInMs = 300
private const val PeekIdleOutMs = 280

/** Fraction of the card height a peek wipe travels (partial = soft glide). */
private const val PeekWipeTravel = 0.45f

/**
 * Small-screen adaptive layout (v6.11). The Spin stack — 44dp spacer +
 * 444dp deck + spin button + Categories/Filter bar — needs ~830dp; on short
 * screens the bottom CTA gets pushed off-screen. When the height the NavHost
 * actually grants this screen (after status bar, bottom nav and gesture
 * insets) drops below this threshold, the page switches to the compact
 * layout: the deck + button move into a vertically scrollable middle band
 * above the bottom CTA, and every fixed size steps down by
 * [SpinCompactDeckScale]. Above the threshold the layout is byte-for-byte the
 * original — normal screens never change.
 */
private val SpinCompactThresholdHeight = 680.dp

/**
 * Extra-compact threshold — screens shorter than this get the smallest
 * Spin tier (v7.2): a smaller deck AND Categories/Filter as tall vertical
 * pills pinned to the left/right screen edges. Implies compact.
 */
private val SpinExtraCompactThresholdHeight = 600.dp

/**
 * Low-density threshold (v7.2) — devices under this density get the
 * compact layout regardless of height (gated by the "Smart density
 * layout" setting since v7.3).
 */
private const val SpinLowDensityDpi = 440

/**
 * High-density threshold (v7.3) — devices at or above this density get the
 * roomy tier (a slightly LARGER deck), so the density rule scales both
 * ways: low dpi → smaller, high dpi → larger.
 */
private const val SpinHighDensityDpi = 440

/** Deck scale factor applied in compact (short-screen) mode. */
private const val SpinCompactDeckScale = 0.88f

/** Deck scale factor applied in extra-compact mode. */
private const val SpinExtraCompactDeckScale = 0.78f

/** Deck scale factor applied in the 2x density tier (v7.4). */
private const val SpinDensityExtraCompactDeckScale = 0.72f

/** Deck scale factor applied in roomy (high-density) mode. */
private const val SpinRoomyDeckScale = 1.05f

@Composable
private fun Carousel(
    cat: CurioCategory,
    deckAccent: Color,
    deckGradient: List<Color>,
    isMixed: Boolean,
    mixSeed: Int,
    displayPool: List<CurioTopic>,
    cycleIndex: Int,
    shuffling: Boolean,
    landedTopic: CurioTopic?,
    opening: Boolean,
    enabled: Boolean,
    compact: Boolean = false,
    extraCompact: Boolean = false,
    densityExtraCompact: Boolean = false,
    roomy: Boolean = false,
    fitScale: Float = 1f,
    onCardTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val poolSize = displayPool.size
    // v6.11 — compact screens shrink the whole fan ~12% so the deck keeps
    // its proportions inside the shorter box; v7.2 — extra-compact scales
    // a further step (~22% total) so the whole fan fits very short screens;
    // v7.3 — roomy scales the fan UP ~5% on high-density screens so the
    // density rule works both ways (low → smaller, high → larger).
    val tierScale = when {
        densityExtraCompact -> SpinDensityExtraCompactDeckScale
        extraCompact -> SpinExtraCompactDeckScale
        compact -> SpinCompactDeckScale
        roomy -> SpinRoomyDeckScale
        else -> 1f
    }
    // v7.15 — the fit scale (space actually available, from SpinScreen's
    // BoxWithConstraints) multiplies the tier scale, so short OR narrow
    // screens compress the fan together with the box below.
    val deckScale = tierScale * fitScale
    Box(
        // v6.3 — grew with the hero ticket so the bigger card keeps its
        // breathing room above/below. The extra-compact box scales with the
        // fan so proportions stay identical; the roomy box grows ~6% to
        // match the up-scaled fan. v7.15 — the whole box also scales by
        // [fitScale] so the fan's layout footprint matches its size.
        modifier = modifier.height(
            when {
                densityExtraCompact -> 325.dp
                extraCompact -> 350.dp
                compact -> 390.dp
                roomy -> 470.dp
                else -> 444.dp
            } * fitScale
        ),
        contentAlignment = Alignment.Center
    ) {
        if (poolSize == 0) {
            EmptyPoolHint(cat)
        } else {
            val slots = listOf(-2, 2, -1, 1, 0)
            slots.forEach { slot ->
                val topic = resolveTopicForSlot(
                    slot = slot,
                    pool = displayPool,
                    cycleIndex = cycleIndex,
                    landedTopic = landedTopic
                )
                if (slot == 0) {
                    HeroTicketCard(
                        accent = deckAccent,
                        gradient = deckGradient,
                        isMixed = isMixed,
                        mixSeed = mixSeed,
                        scale = deckScale,
                        topic = topic,
                        cat = cat,
                        landed = landedTopic != null,
                        shuffling = shuffling,
                        opening = opening,
                        // The front card can show an idle-deck topic before the
                        // first shuffle, so its tap target must follow the
                        // rendered card instead of requiring a landed topic.
                        enabled = enabled && topic != null,
                        onTap = onCardTap
                    )
                } else {
                    PeekCard(
                        slot = slot,
                        scale = deckScale,
                        accent = deckAccent,
                        gradient = deckGradient,
                        cat = cat,
                        topic = topic,
                        shuffling = shuffling
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPoolHint(cat: CurioCategory) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerLow),
            shadowElevation = 0.dp,
            border = cat.categoryBorder(),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CurioIcon(
                    cat.iconGlyph, null,
                    tint = cat.categoryInk().copy(alpha = 0.5f),
                    size = 56.dp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Nothing here yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "This lane is still forming — new topics will appear here as you explore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HeroTicketCard(
    accent: Color,
    gradient: List<Color>,
    isMixed: Boolean,
    mixSeed: Int,
    scale: Float = 1f,
    topic: CurioTopic?,
    cat: CurioCategory,
    landed: Boolean,
    shuffling: Boolean,
    opening: Boolean,
    enabled: Boolean,
    onTap: () -> Unit
) {
    // ── Shared-element handoff (Topic Reveal morph) ──────────────────────
    // This front ticket is the source of the "reveal-hero" shared element:
    // the reveal destination provides the matching hero, so opening the
    // landed topic morphs the hero OUT of this card. The scopes are provided
    // by the NavHost via composition locals (never null in its subtree).
    val sharedTransitionScope = LocalRevealSharedScope.current ?: return
    val animatedVisibilityScope = LocalRevealVisibilityScope.current ?: return
    val revealSharedState = sharedTransitionScope.rememberSharedContentState(RevealSharedElementKey)

    // v6.3 — slightly bigger ticket (~6% up) so the hero card reads a
    // touch more prominent on the deck.
    // v6.11 — compact screens scale the whole ticket down (small phones);
    // proportions and internal paddings stay identical.
    val w = 286.dp * scale
    val h = 310.dp * scale
    // Mixed decks render the multi-accent stops in a non-linear arrangement
    // (diagonal sweep / reversed diagonal / radial glow, keyed off the
    // deck's category set); single decks keep the plain vertical theme-aware
    // card gradient. Built at the card's pixel size so the brush geometry
    // matches the ticket exactly.
    val density = LocalDensity.current
    val wPx = with(density) { w.toPx() }
    val hPx = with(density) { h.toPx() }
    // v7.13 — Main card toggles read directly from reactive state so
    // flipping any toggle recomposes the hero card instantly.
    val heroGradientOn = AppPreferences.heroGradientState
    val heroBorderOn = AppPreferences.heroBorderState
    val heroShadowOn = AppPreferences.heroShadowState
    // v7.14 — the enhanced gradient is a top-left-lit DIAGONAL multi-stop
    // sweep: a bright crown at the top-left catches light, the card's own
    // stops run through the middle (the Material blend keeps its identity),
    // and a deepened base at the bottom-right grounds the card. The classic
    // two-stop fallback gains an HSL-smooth midpoint so the light→base
    // glide never bands through muddy grey. Pastel light mode softens the
    // crown so the pale fill doesn't wash to white.
    val dark = isCurioDarkTheme()
    val pastelLightHero = AppPreferences.pastelColorsState && !dark
    val ticketBrush = if (isMixed) {
        CurioMixedDeck.mixedDeckHeroBrush(gradient, wPx, hPx, mixSeed)
    } else if (heroGradientOn) {
        val crown = lerp(gradient.first(), Color.White, if (pastelLightHero) 0.08f else 0.16f)
        val base = lerp(gradient.last(), Color.Black, 0.06f)
        val stops = if (gradient.size > 2) {
            listOf(crown) + gradient.drop(1).dropLast(1) + listOf(base)
        } else {
            CurioGradients.hslGradientStops(crown, base, 3)
        }
        Brush.linearGradient(
            stops,
            start = Offset(0f, 0f),
            end = Offset(wPx, hPx)
        )
    } else {
        Brush.verticalGradient(gradient)
    }
    // v7.5 — pastel mode lightens the ticket gradient, so the content ink
    // flips from white to a deep ink of the deck color (light mode) / a
    // light tint (dark). White when pastel mode is off.
    // v7.16 — pastel ink refined: ALWAYS derived from the deck accent via
    // pastelFillInk in pastel mode (single + mixed decks alike), so every
    // card reads with the same calm deep-hue ink instead of the raw 700
    // accent or the fixed DeepPlum special-case for pale accents — no hue
    // surprises on the card, and it matches every other pastel surface.
    val ink = if (AppPreferences.pastelColorsState) pastelFillInk(accent) else cat.onAccent()

    // ── Opening handoff — NO pre-grow. The shared-element morph (see
    //    CurioNavHost) IS the expansion. A pre-grow made the visual card
    //    larger than its layout bounds, so the overlay started at the
    //    wrong position and the morph read as disconnected. The only job
    //    here is the brief "Opening…" label before navigating.
    val openingScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(CurioMotion.Durations.RevealHold),
        label = "openingCardScale"
    )

    // ── Per-tick shuffle pulse — the front card bounces in sync with the
    //    wheel: every time the displayed topic switches, the card kicks
    //    instantly to peak scale then springs back down, rocking side to
    //    side. Even the fastest early ticks visibly jump (rhythmic pulse);
    //    the slower deceleration ticks ring out as full, readable bounces.
    //    The tilt alternates direction each tick so the rock feels organic
    //    instead of a one-way drift (the old per-topic hash rotation jumped
    //    randomly, which read as jitter).
    val tickPulse = remember { Animatable(1f) }
    var tickDir by remember { mutableStateOf(1f) }
    LaunchedEffect(topic?.id, shuffling) {
        if (!shuffling || topic == null) return@LaunchedEffect
        tickDir = -tickDir
        // v6.6 — calm breath instead of a kick: the card lifts barely
        // (1.02) and glides back on a heavily damped, low-stiffness
        // spring, so each tick reads as a soft pulse, never a slam.
        tickPulse.snapTo(1.02f)
        tickPulse.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 420f))
    }

    // ── Category switch — one welcoming bounce as the deck re-fans to the
    //    new category's topics (also fires on first mount).
    LaunchedEffect(cat.id) {
        if (!shuffling && !landed) {
            tickPulse.snapTo(1f)
            tickPulse.animateTo(1.025f, CurioMotion.Springs.Bouncy)
            tickPulse.animateTo(1f, CurioMotion.Springs.Elastic)
        }
    }

    // ── Landing settle — seamless handoff from the shuffle tick pulse to
    //    the elastic rest spring. On landing, snap to wherever the pulse
    //    left off (zero visual jump) then spring down to rest scale.
    val settleScale = remember { Animatable(1f) }
    val settleY = remember { Animatable(0f) }
    // True when the ticket enters composition already in the landed state —
    // a pop back from Topic Reveal, or a restored tab. The landing grow is
    // only animated when the wheel lands DURING this composition; a fresh
    // landed ticket snaps straight to rest so the reversing hero morph
    // lands on a stable target (the old replay grew the card 1→1.02 under
    // the overlay — the "back animation starts at the wrong size" artifact).
    val landedOnEntry = remember { landed }

    // Snap both to the pulse's last position on landing (zero visual jump),
    // reset to rest when a new shuffle begins.
    LaunchedEffect(landed) {
        if (landed) {
            settleScale.snapTo(tickPulse.value)
            settleY.snapTo(-(tickPulse.value - 1f) * 12f)
            if (landedOnEntry) {
                // Already at rest — arrive exactly there, no grow animation.
                settleScale.snapTo(LandedRestScale)
                settleY.snapTo(0f)
            } else {
                // Settle scale + vertical position in parallel (separate
                // coroutines) so the card lands as one unified glide, not
                // two sequential springs. v6.6 — the landing settle uses the
                // controlled Deliberate spring (85% damping, no bounce)
                // instead of the extreme Elastic overshoot, so the wheel's
                // stop reads as a confident rest, not a violent bounce.
                launch { settleScale.animateTo(LandedRestScale, CurioMotion.Springs.Deliberate) }
                launch { settleY.animateTo(0f, CurioMotion.Springs.Deliberate) }
            }
        } else {
            settleScale.snapTo(1f)
            settleY.snapTo(0f)
        }
    }
    // When the user taps to open, settle back to EXACT scale 1 before the
    // shared-element morph captures bounds: the overlay animates the LAYOUT
    // bounds (scale 1.0), so a card still resting at LandedRestScale (1.02)
    // would start the morph 2% smaller than the visible card.
    LaunchedEffect(opening) {
        if (opening) settleScale.animateTo(1f, CurioMotion.Springs.Deliberate)
    }

    // Outer Box padded 12dp beyond card for shadow breathing room.
    // Inner clip layer keeps rounded corners crisp during scale.
    Box(
        modifier = Modifier
            .size(w + 24.dp, h + 24.dp)
            .graphicsLayer {
                // Idle and shuffling both track tickPulse (rest = exactly 1f);
                // the category-switch + per-tick bounces ride on it, and the
                // landing handoff snaps to whatever value it left off at.
                val baseScale = if (landed) settleScale.value else tickPulse.value
                scaleX = baseScale * openingScale
                scaleY = baseScale * openingScale
                // v6.6 — the per-tick rock is a gentle tilt now (16° vs the
                // old 40°) so the card breathes instead of whipping side to
                // side, and the vertical hop shrinks to match.
                rotationZ = if (shuffling) (tickPulse.value - 1f) * 16f * tickDir else 0f
                translationY = if (landed) settleY.value else -(tickPulse.value - 1f) * 12f
            }
            .zIndex(10f)
            .then(
                if (enabled) Modifier.clickable(
                    indication = null,
                    interactionSource = null,
                    onClick = onTap
                ) else Modifier
            )
    ) {
        // Inner clip layer centered in outer box — prevents sharp edges
        // during scale. The layered hero shadow sits OUTSIDE the clip (its
        // modifier comes before .clip), so it renders around the card
        // instead of being swallowed by the rounded clip.
        Box(
            modifier = Modifier
                .size(w, h)
                .align(Alignment.Center)
                .then(
                    if (topic != null) {
                        // Shared-element source for the Topic Reveal hero —
                        // when this ticket is tapped (or the wheel lands),
                        // the reveal's hero expands out of this card's
                        // position instead of the page sliding in.
                        sharedTransitionScope.run {
                            Modifier.sharedElement(
                                revealSharedState,
                                animatedVisibilityScope,
                                boundsTransform = RevealBoundsTransform
                            )
                        }
                    } else Modifier
                )
                .then(
                    if (heroShadowOn) {
                        // v7.14 — layered soft shadow: a broad ambient glow
                        // tinted with the card's accent (the card lifts with
                        // a hint of its own hue) plus a tight, dark contact
                        // shadow that grounds it — two distinct depths
                        // instead of one flat elevation. clip=false keeps
                        // both shadows visible around the rounded shape.
                        Modifier
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(30.dp),
                                ambientColor = accent.copy(alpha = 0.30f),
                                spotColor = accent.copy(alpha = 0.34f),
                                clip = false
                            )
                            .shadow(
                                elevation = 5.dp,
                                shape = RoundedCornerShape(30.dp),
                                ambientColor = Color.Black.copy(alpha = 0.28f),
                                spotColor = Color.Black.copy(alpha = 0.40f),
                                clip = false
                            )
                    } else Modifier
                )
                .clip(RoundedCornerShape(30.dp))
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = Color.Transparent,
                // v7.14 — elevation shadow moved to the layered
                // Modifier.shadow chain above; the Surface stays flat.
                shadowElevation = 0.dp,
                // v7.16 — refined border: OFF is a whisper 1dp hairline at
                // 18% ink — seats the card on the page without a harsh
                // accent outline (the accent rim-light stays OFF by default).
                // ON drops the hairline lower and the accent is drawn as a
                // soft gradient rim-light inside (the drawBehind below).
                border = BorderStroke(
                    1.dp,
                    ink.copy(alpha = if (heroBorderOn) 0.14f else 0.18f)
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            ticketBrush,
                            RoundedCornerShape(30.dp)
                        )
                        .then(
                            if (heroBorderOn) {
                                // v7.16 — refined gradient rim-light: a slim
                                // 1.5dp stroke that catches light at the top
                                // (barely brightened ink) and eases a whisper
                                // of the accent at the bottom, plus a fainter
                                // 1dp bevel hairline just inside — a soft
                                // machined edge instead of the old harsh 2dp
                                // white-to-accent ring.
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
                                    val innerW = 1.dp.toPx()
                                    val inset = borderW + innerW
                                    drawRoundRect(
                                        // Bevel hairline: dark in light mode;
                                        // in dark mode a black line would
                                        // vanish on the dark fill, so it flips
                                        // to a faint light stroke instead.
                                        color = if (dark)
                                            lerp(ink, Color.White, 0.60f).copy(alpha = 0.06f)
                                        else
                                            Color.Black.copy(alpha = 0.04f),
                                        topLeft = Offset(inset, inset),
                                        size = Size(size.width - 2f * inset, size.height - 2f * inset),
                                        cornerRadius = CornerRadius(radius - innerW, radius - innerW),
                                        style = Stroke(width = innerW)
                                    )
                                }
                            } else Modifier
                        )
                ) {
                    // One category watermark — keep the Shuffle hero focused
                    // on the active deck instead of repeating the page-wide
                    // glyph collage. Mixed decks use their synthetic spark
                    // category here, just as they did before the pattern pass.
                    CurioIcon(
                        name = cat.iconGlyph,
                        contentDescription = null,
                        tint = ink.copy(alpha = 0.16f),
                        size = 150.dp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                    )

                    // ── Creator byline pill — "Director · Nolan" pinned to
                    //    the ticket's TOP corner (the band the old subtype
                    //    badge owned — the content column's 28dp spacer keeps
                    //    the title clear of it). Same tag language as the
                    //    Topic Reveal pill and the Home tag pills: the card's
                    //    ink on a soft tinted pill, so the work's creator
                    //    reads at a glance while the deck reels. Label comes
                    //    from the TOPIC's own category so mixed decks stay
                    //    correct (a book in a films+books mix says "Author",
                    //    not "Director"). Artworks = painter (their names no
                    //    longer carry the trailing "by …"), albums = artist,
                    //    discoveries = discoverer ("Discovered by · Fleming").
                    val byline = topic?.byline?.takeIf { it.isNotBlank() }
                    val bylineLabel = when (topic?.categoryId) {
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
                                .align(Alignment.TopStart)
                                .padding(20.dp)
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

                    // ── Content column ─────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // v7.14 — the subtype-badge row used to occupy the
                        // top of the card (it moved to the topic detail),
                        // keeping the topic title in the balanced middle
                        // third. With it gone, SpaceBetween slides the title
                        // to the very top edge; a spacer the height of the
                        // old badge (~28dp) returns it to that position.
                        Spacer(Modifier.height(28.dp))

                        // Name + tags + teaser — v6.6: reels with the deck.
                        // Previously the hero content snapped instantly on
                        // every tick; now it glides like a card rising from
                        // the back of the deck to the front — incoming
                        // content slides up from the lower edge while the
                        // outgoing exits upward, eased so each tick is a
                        // readable glide instead of a hard cut.
                        AnimatedContent(
                            targetState = topic,
                            transitionSpec = {
                                if (shuffling) {
                                    // v6.10 — same rhythm as the peek wipes
                                    // (under the 200ms tick floor) with a
                                    // stronger height/2 rise so the front
                                    // content visibly glides up as the deck
                                    // streams. clip=false keeps the title
                                    // reel from being sliced mid-slide.
                                    (slideInVertically(
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ) { height -> height / 2 } +
                                        fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))) togetherWith
                                    (slideOutVertically(
                                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                                    ) { height -> -height / 2 } +
                                        fadeOut(animationSpec = tween(260, easing = FastOutSlowInEasing))) using SizeTransform(clip = false)
                                } else {
                                    (slideInVertically(
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ) { height -> height / 4 } +
                                        fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))) togetherWith
                                    (slideOutVertically(
                                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                                    ) { height -> -height / 4 } +
                                        fadeOut(animationSpec = tween(260, easing = FastOutSlowInEasing))) using SizeTransform(clip = false)
                                }
                            },
                            label = "heroContentReel"
                        ) { currentTopic ->
                        Column {
                            Text(
                                text = currentTopic?.name ?: "Ready when you are",
                                // v7.16 — enhanced typography is now the
                                // shipped default: a true display treatment —
                                // ExtraBold geom at 34sp with negative
                                // tracking and a tight line height, so the
                                // topic reads as an editorial headline
                                // instead of a stock M3 headline.
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 38.sp,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = ink,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                // The title is NOT a shared element anymore:
                                // text can't morph cleanly in
                                // SharedTransitionScope (it scales the stable
                                // layout — the 34sp card title stretched to
                                // the full-width headline and long names
                                // warped). The reveal headline fades in on
                                // its own; the hero card is the only shared
                                // piece.
                                modifier = Modifier
                            )
                            if (currentTopic != null && currentTopic.tags.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    currentTopic.tags.take(2).forEach { tag ->
                                        // v7.16 — enhanced tags are the shipped
                                        // default: accent-tinted pills with
                                        // tracked bold labels, so the genre/era
                                        // chips read as designed details rather
                                        // than flat text plates.
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = lerp(ink, accent, 0.30f).copy(alpha = 0.22f)
                                        ) {
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.3.sp
                                                ),
                                                color = ink,
                                                modifier = Modifier.padding(
                                                    horizontal = 12.dp,
                                                    vertical = 5.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            if (currentTopic != null && landed) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = currentTopic.teaser,
                                    // v7.16 — enhanced teaser is the shipped
                                    // default: one step up in size and line
                                    // height with a brighter ink.
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    ),
                                    color = ink.copy(alpha = 0.92f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        }

                        // Tap hint — the card is ALWAYS openable (v7.106):
                        // "Tap to open" at rest, "Shuffling…" while the wheel
                        // runs, and a pulsing "Opening…" during the brief
                        // opening handoff.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (opening) {
                                OpeningPulseDot(tint = ink)
                                Text(
                                    text = "Opening…",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ink.copy(alpha = 0.88f)
                                )
                            } else {
                                CurioIcon(
                                    if (shuffling) CurioIcons.Casino else CurioIcons.ChevronRight, null,
                                    tint = ink,
                                    size = 16.dp
                                )
                                Text(
                                    text = if (shuffling) "Shuffling…" else "Tap to open",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ink.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Slim "deck" peek card fanned behind the hero ticket — hints at the
 * neighboring topic from the visible edge (top peek = next up, bottom
 * peek = previous).
 */
@Composable
private fun PeekCard(
    slot: Int,
    scale: Float = 1f,
    accent: Color,
    // The deck's TRUE gradient stops — the multi-accent sweep for mixed
    // decks (same family the hero ticket wears), the theme-aware card
    // gradient otherwise. The peeks used to rebuild a single-accent
    // cardGradient(accent) from the blended color, which flattened a
    // mixed deck's gradient into one flat hue.
    gradient: List<Color>,
    cat: CurioCategory,
    topic: CurioTopic?,
    shuffling: Boolean
) {
    val isTop = slot < 0
    val far = kotlin.math.abs(slot) == 2
    // Slightly lower + wider fan: the whole deck sits a few px closer to
    // the spin button and the far pair is spread a touch more so each
    // layer reads as a separate card instead of one blurred pile.
    // v6.11 — compact screens scale the fan offsets + card sizes down so
    // the deck keeps the same look, just tighter on short screens.
    val yOff = when (slot) {
        -2 -> -178f * scale
        -1 -> -134f * scale
        1 -> 146f * scale
        else -> 188f * scale
    }
    // v6.5 — peek cards grew ~13% so the topic title inside each background
    // card has room to read instead of hiding behind the fan. Proportions
    // are kept — only the overall size went up, never the shape.
    val w = (if (far) 328.dp else 360.dp) * scale
    val h = (if (far) 96.dp else 116.dp) * scale
    // Corner radius scales with card height so the slim far deck cards
    // keep crisp, proportional corners instead of over-rounded ones.
    val corner = (if (far) 15.dp else 19.dp) * scale
    // Level-based shading — near cards step one shade down from the hero,
    // far cards step down again, so the deck fades into the background in
    // distinct layers. White content stays readable on the dimmed fill.
    // Mixed decks shade the blended accent so the whole deck reads mixed.
    // v7.7 — pastel mode keeps the peeks in the pastel CARD family instead
    // of the old flat mid-tones (which read neither pastel nor deep accent).
    // v7.15 — pastel depth now steps via HSL lightness drop (below), not
    // black-lerp, so the airy pastels stay in family.
    val pastelMode = AppPreferences.pastelColorsState
    val darkMode = isCurioDarkTheme()
    // v7.14 — peek cards wear the SAME card-gradient family as the hero
    // (the Material card blend when active, the classic category gradient
    // otherwise), stepped a level DARKER so the deck keeps its hierarchy
    // (hero brightest, near a step down, far a step further).
    // v7.15 — peeks wear the deck's OWN gradient stops (multi-accent sweep
    // for mixed decks, theme-aware card gradient otherwise) instead of
    // rebuilding a flat single-accent cardGradient from the blend — so a
    // mixed deck's peeks read mixed, not one flat hue.
    val blendStops = gradient
    // v7.17 — CALM peeks in every palette: the deck-gradient family read
    // bright & vibrant on the slim background cards in ALL modes (default
    // Curio, Material's device-primary blend, pastel). The peeks now step
    // clearly deeper AND desaturate so they recede behind the hero ticket:
    //  - non-pastel: black-lerp deepened (near 0.40 / far 0.52, was
    //    0.28/0.42) plus an HSL saturation pull (~0.80x, capped at 0.50)
    //    to kill the vividness of the saturated device/category stops.
    //  - pastel: a heavier lightness drop (light near 0.12 / far 0.18,
    //    dark 0.11/0.16 — was 0.06/0.10 and 0.09/0.14) with the same
    //    gentle saturation pull, so pale peeks stay in family but read a
    //    clear step below the hero instead of glowing beside it.
    val cardStops = remember(blendStops, far, pastelMode, darkMode) {
        if (pastelMode) {
            // Pastel peeks stay IN FAMILY: step the depth by dropping
            // LIGHTNESS (HSL) instead of black-lerping, which greyed the
            // airy pastels into muddy mids. Hue is held; saturation is
            // pulled down slightly so the peeks read calm, not glowing.
            blendStops.map { stop ->
                val h = toHsl(stop)
                val drop = if (far) {
                    if (darkMode) 0.16f else 0.18f
                } else {
                    if (darkMode) 0.11f else 0.12f
                }
                fromHsl(
                    h.h,
                    // v7.18 — 5% less saturated: pull eased 0.85x → 0.80x.
                    (h.s * 0.80f).coerceAtMost(0.45f),
                    (h.l - drop).coerceIn(0f, 1f)
                )
            }
        } else {
            // Non-pastel peeks deepen MORE and pull saturation down so the
            // vivid device/category stops recede quietly behind the hero
            // instead of shining.
            val level = if (far) 0.52f else 0.40f
            blendStops.map { stop ->
                val c = lerp(stop, Color.Black, level)
                val h = toHsl(c)
                // v7.18 — 5% less saturated: pull eased 0.80x → 0.75x.
                fromHsl(h.h, (h.s * 0.75f).coerceAtMost(0.50f), h.l)
            }
        }
    }
    // v7.5 — pastel mode lightens the peek fill, so content flips to a deep
    // ink of the deck color (light mode) / a light tint (dark).
    val ink = pastelFillInk(accent)

    // v7.7 — deck card redesign (EXPERIMENTAL, four independent Settings
    // toggles, each OFF by default): the blend gradient is now the BASE
    // peek fill, the generic hairline is tinted with the category's own
    // colors, near cards gain soft shadows, and near titles get two
    // readable lines. Reads each reactive preference directly so flipping
    // any toggle recomposes the deck instantly; when a flag is OFF its
    // feature resolves to the classic look.
    val gradientOn = AppPreferences.peekGradientState
    val hairlineOn = AppPreferences.peekHairlineState
    val shadowsOn = AppPreferences.peekShadowsState
    val titlesOn = AppPreferences.peekTitlesState
    // 1a — top-lit crown: a whisper of light at the card top so the top
    // peek catches light and whispers "next up" on the reel. The base is
    // always the level-darkened blend; the gradient toggle layers the
    // crown lighten on top.
    val fillBrush = remember(cardStops, far, pastelMode, darkMode, gradientOn) {
        val base = Brush.verticalGradient(cardStops)
        if (!gradientOn) return@remember base
        // v7.17 — the top-lit crown is now a WHISPER (was 0.10-0.14
        // white-lerp) so the gradient keeps its light feel without adding
        // brightness to the already-deepened, desaturated peek fills.
        val crown = if (pastelMode && !darkMode) lerp(cardStops.first(), Color.White, 0.05f)
                    else lerp(cardStops.first(), Color.White, if (far) 0.04f else 0.06f)
        Brush.verticalGradient(listOf(crown) + cardStops.drop(1))
    }
    // 1b — category-tinted hairline so each deck layer whispers its
    // category instead of a generic white rule. The light twin reads on the
    // DARK deck fills (both non-pastel light and dark mode); in pastel
    // light mode the fills are pale, so the deep accent ink carries the
    // edge instead (a deep-on-deep hairline would vanish — reviewer catch).
    val hairline = if (hairlineOn) {
        if (pastelMode && !darkMode) {
            cat.categoryInk().copy(alpha = if (far) 0.22f else 0.30f)
        } else {
            cat.lightAccent.copy(alpha = if (far) 0.28f else 0.40f)
        }
    } else {
        ink.copy(alpha = if (far) 0.14f else 0.22f)
    }

    Box(
        modifier = Modifier
            .size(w, h)
            // v7.38 — LAYERED soft shadow (the same recipe as the hero
            // ticket): a broad ambient glow tinted with the card's accent
            // plus a tight dark contact shadow, both drawn OUTSIDE the
            // clip. The old single Surface shadowElevation read as a hard
            // dark halo hugging every fanned card — heavy black rings on
            // the deck that muddied the layers. The two-depth recipe reads
            // as a gentle lift: a soft wash of the card's own hue + a
            // whisper of grounding shadow. Far cards sit lower (smaller,
            // fainter) than near cards so the deck keeps its depth order.
            .then(
                if (shadowsOn) {
                    Modifier
                        .shadow(
                            elevation = if (far) 10.dp else 14.dp,
                            shape = RoundedCornerShape(corner),
                            ambientColor = accent.copy(alpha = if (far) 0.10f else 0.14f),
                            spotColor = accent.copy(alpha = if (far) 0.12f else 0.16f),
                            clip = false
                        )
                        .shadow(
                            elevation = if (far) 3.dp else 5.dp,
                            shape = RoundedCornerShape(corner),
                            ambientColor = Color.Black.copy(alpha = if (far) 0.12f else 0.18f),
                            spotColor = Color.Black.copy(alpha = if (far) 0.16f else 0.24f),
                            clip = false
                        )
                } else Modifier
            )
            .graphicsLayer {
                translationY = yOff.dp.toPx()
                rotationZ = when (slot) { -2 -> -3.5f; -1 -> -1.4f; 1 -> 1.4f; else -> 3.5f }
                scaleX = if (far) 0.92f else 0.98f
                scaleY = if (far) 0.92f else 0.98f
                // Fully opaque — translucent layers blend badly with the tilt
                // and render the card as soft/pixelated. Depth comes from
                // scale + rotation + zIndex instead of transparency.
                alpha = 1f
            }
            .zIndex(if (far) 2f else 5f)
    ) {
        AnimatedContent(
            targetState = topic,
            transitionSpec = {
                // v7.1 — direction + softness. Top peek cards (title placed
                // at the card top) feed the deck from ABOVE — their content
                // DROPS down into the card — while bottom peeks rise up, so
                // the fan streams toward the hero from both ends and a top
                // card's title is never sliced off the top edge by the old
                // upward wipe (which read as cut off). The wipe itself is a
                // partial-height glide + fade (like the hero's content reel)
                // instead of a full-height hard slot cut, and the durations
                // sit UNDER the ~340ms tick floor so each step completes
                // before the next tick lands.
                val dir = if (isTop) -1f else 1f
                if (shuffling) {
                    slideInVertically(
                        animationSpec = tween(PeekWipeInMs, easing = FastOutSlowInEasing)
                    ) { height -> (height * dir * PeekWipeTravel).toInt() } +
                    fadeIn(animationSpec = tween(PeekWipeInMs, easing = FastOutSlowInEasing)) togetherWith
                    slideOutVertically(
                        animationSpec = tween(PeekWipeOutMs, easing = FastOutSlowInEasing)
                    ) { height -> (height * -dir * PeekWipeTravel).toInt() } +
                    fadeOut(animationSpec = tween(PeekWipeOutMs, easing = FastOutSlowInEasing)) using SizeTransform(clip = false)
                } else {
                    // Idle re-fan (landing re-deal / category switch) — a
                    // slower, softer pass in the same per-side direction.
                    slideInVertically(
                        animationSpec = tween(PeekIdleInMs, easing = FastOutSlowInEasing)
                    ) { height -> (height * dir * PeekWipeTravel).toInt() } +
                    fadeIn(animationSpec = tween(PeekIdleInMs, easing = FastOutSlowInEasing)) togetherWith
                    slideOutVertically(
                        animationSpec = tween(PeekIdleOutMs, easing = FastOutSlowInEasing)
                    ) { height -> (height * -dir * PeekWipeTravel).toInt() } +
                    fadeOut(animationSpec = tween(PeekIdleOutMs, easing = FastOutSlowInEasing)) using SizeTransform(clip = false)
                }
            },
            label = "peekSlot_$slot"
        ) { currentTopic ->
            Surface(
                shape = RoundedCornerShape(corner),
                // v7.14 — the fill is always the level-darkened blend brush,
                // so the Surface stays transparent and the brush (applied
                // below) is what the eye sees.
                color = Color.Transparent,
                // v7.38 — the layered shadow now lives on the outer Box
                // modifier (above, before the clip); the Surface stays flat
                // so the two-depth glow is the ONLY shadow and never stacks
                // with an elevation halo.
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                // Subtle hairline outline — kept very light so the rotated
                // stroke stays crisp instead of aliasing into pixel noise —
                // lets each deck layer read as a distinct card.
                border = BorderStroke(width = 1.dp, color = hairline),
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = fillBrush, shape = RoundedCornerShape(corner))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = if (isTop) Arrangement.Top else Arrangement.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurioIcon(
                            name = cat.iconGlyph,
                            contentDescription = null,
                            tint = ink.copy(alpha = if (far) 0.55f else 0.75f),
                            size = if (titlesOn) (if (far) 18.dp else 22.dp) else 20.dp
                        )
                        Text(
                            text = currentTopic?.name ?: "…",
                            style = if (titlesOn) {
                                if (far) {
                                    // 3 — far cards are hints, not reads: a
                                    // smaller, softer single line.
                                    MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    // 3 — near titles breathe: 16sp SemiBold
                                    // with light tracking and TWO lines so
                                    // long topic names stop clipping at one.
                                    MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.15.sp
                                    )
                                }
                            } else {
                                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            },
                            // Far deck cards dim their content too, reinforcing
                            // the layered fade into the background.
                            color = if (titlesOn && far) ink.copy(alpha = 0.72f)
                                    else ink.copy(alpha = if (far) 0.65f else 1f),
                            maxLines = if (titlesOn && !far) 2 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
// ═══════════════════════════════════════════════════════════════════════════
// Center spin button (with optional orbit ring)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SpinButton(
    tint: Color,
    isShuffling: Boolean,
    landedTopic: CurioTopic?,
    pulseScale: Float,
    enabled: Boolean,
    compact: Boolean = false,
    fitScale: Float = 1f,
    onClick: () -> Unit
) {
    // v6.3 — button grew a little (~7% up): 126dp idle, 108dp landed.
    // v6.11 — compact screens step the button + orbit down ~11% so the
    // pinned Categories/Filter bar always stays on screen.
    // v7.x — the button also rides the continuous fit scale: on small
    // screens the deck compresses via fitScale but the button used to stay
    // full-size, so it read oversized next to the shrunken fan. It now
    // shrinks WITH the deck (the orbit ring too), floored at 0.75 so the
    // CTA never gets tiny.
    val sizeScale = fitScale.coerceIn(0.75f, 1f)
    val buttonSize = (if (compact) {
        if (landedTopic != null) 96.dp else 112.dp
    } else {
        if (landedTopic != null) 108.dp else 126.dp
    }) * sizeScale
    Box(
        modifier = Modifier.size((if (compact) 156.dp else 176.dp) * sizeScale),
        contentAlignment = Alignment.Center
    ) {
        OrbitRing(active = isShuffling, color = tint, modifier = Modifier.fillMaxSize())
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            // v7.11 — 3D button gradient (Settings toggle, default ON): the
            // shuffle button wears a radial gradient with a highlight toward
            // the top and a shadow toward the bottom, with a soft ambient
            // shadow, so it reads as a raised sphere instead of a flat circle.
            // When OFF, the button keeps its classic flat accent fill.
            color = if (AppPreferences.threeDButtonState) Color.Transparent else tint,
            shadowElevation = if (AppPreferences.threeDButtonState) 6.dp else 0.dp,
            modifier = Modifier
                .size(buttonSize)
                .scale(pulseScale.coerceIn(0.9f, 1.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (AppPreferences.threeDButtonState) {
                            // v7.14 — the pastel-light button already wears an
                            // airy near-white accent, so the old 22% white
                            // highlight turned the top into a white cap; drop
                            // it to a soft 12% in pastel light mode so the
                            // sphere keeps a hint of shine without washing
                            // out, while darker fills keep their stronger cap.
                            val pastelLight = AppPreferences.pastelColorsState && !isCurioDarkTheme()
                            val highlight = lerp(tint, Color.White, if (pastelLight) 0.12f else 0.22f)
                            Modifier.background(
                                Brush.radialGradient(
                                    listOf(highlight, tint, lerp(tint, Color.Black, 0.07f)),
                                    center = Offset(0.42f, 0.33f),
                                    radius = 1.15f
                                ),
                                CircleShape
                            )
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                // v5.10 — the dice shows in EVERY state: tumbling while
                // shuffling, a steady white dice on the filled accent.
                // v6.10 — the tumble MORPHS into the resting dice (spring
                // scale + fade) instead of hard-swapping, so the end of a
                // spin reads as the die settling — never an abrupt stop.
                AnimatedContent(
                    targetState = isShuffling,
                    transitionSpec = {
                        (scaleIn(
                            initialScale = 0.55f,
                            animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f)
                        ) + fadeIn(animationSpec = tween(170))) togetherWith
                        (scaleOut(
                            targetScale = 0.55f,
                            animationSpec = tween(150)
                        ) + fadeOut(animationSpec = tween(150)))
                    },
                    label = "diceMorph"
                ) { shuffling ->
                    if (shuffling) {
                        ShuffleGlyph(
                            tint = pastelFillInk(tint),
                            modifier = Modifier
                                .size(72.dp)
                                // Keep the animated die on the same optical
                                // center as the resting casino glyph.
                                .offset(y = (-1f).dp)
                        )
                    } else {
                        // Gentle idle breathe on the resting die — a slow,
                        // even pulse so the settled dice stays alive.
                        val idleBreathe = rememberInfiniteTransition(label = "diceIdle")
                        val breathe by idleBreathe.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "diceIdleBreathe"
                        )
                        CurioIcon(
                            CurioIcons.Casino, null,
                            tint = pastelFillInk(tint),
                            size = if (landedTopic != null) 52.dp else 60.dp,
                            // Optical correction for the casino glyph's
                            // visible bounds. The parent Box and button are
                            // already centered; only the die's ink needs a
                            // tiny lift, including the idle Spin state.
                            modifier = Modifier
                                .offset(y = (-1f).dp)
                                .graphicsLayer {
                                    scaleX = 1f + breathe * 0.05f
                                    scaleY = 1f + breathe * 0.05f
                                }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rotating ring of dots around the spin button during shuffle — bigger,
 * shimmering dots that fade/scatter in when a spin starts and out when it
 * ends, so the band reads as living light around the button.
 */
@Composable
private fun OrbitRing(active: Boolean, color: Color, modifier: Modifier = Modifier) {
    // v7.11 — in pastel mode the raw accent color blends into the pastel
    // background (invisible dots). pastelFillInk resolves to a deep hue ink
    // in pastel light mode and a light tint in pastel dark mode, so the
    // orbiting dots stay readable on every background. Non-pastel keeps
    // the classic accent-colored dots.
    val dotColor = pastelFillInk(color)
    AnimatedVisibility(
        visible = active,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(220)) +
            scaleIn(initialScale = 0.55f, animationSpec = tween(240, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        val infinite = rememberInfiniteTransition(label = "orbit")
        val rot by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing)
            ),
            label = "orbitRot"
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = (size.minDimension / 2f) - 8f.dp.toPx()
            // v7.x — dots grown from 3dp to 4.5dp so the orbit band reads
            // clearly instead of a faint speckle.
            val dotR = 4.5f.dp.toPx()
            val n = 10
            val rotRad = rot * (Math.PI.toFloat() / 180f)
            rotate(degrees = rot, pivot = Offset(cx, cy)) {
                for (i in 0 until n) {
                    val a = (i.toFloat() / n) * (2f * Math.PI.toFloat())
                    val center = Offset(cx + cos(a) * radius, cy + sin(a) * radius)
                    // Shimmer: each dot breathes with a phase offset while
                    // the ring orbits, so the band reads as living light
                    // instead of a rigid necklace.
                    val pulse = (sin(rotRad * 1.4f + i * 1.15f) + 1f) / 2f
                    val r = dotR * (0.7f + 0.5f * pulse)
                    // Soft glow behind the dot.
                    drawCircle(
                        color = dotColor.copy(alpha = 0.14f + 0.22f * pulse),
                        radius = r * 1.9f,
                        center = center
                    )
                    drawCircle(
                        color = dotColor.copy(alpha = 0.4f + 0.6f * pulse),
                        radius = r,
                        center = center
                    )
                }
            }
        }
    }
}

@Composable
private fun ShuffleGlyph(tint: Color, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "shuffleGlyph")
    // v5.10 — smooth, unhurried tumble: LinearEasing wraps 360°→0° with no
    // visible snap (the old FastOutSlowIn + Restart eased out then jumped
    // back, which read as fast and janky). The dot pattern is rotationally
    // symmetric, so the wrap-around is invisible — a true seamless loop.
    // 1600ms per turn completes ~1.5–2 rotations inside the 2.4–3.2s
    // shuffle window — fluid, never frantic, and never stalled.
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing)
        ),
        label = "shuffleAngle"
    )
    // Gentle breathe so the die feels alive while it rolls.
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shufflePulse"
    )
    // v6.10 — a slow vertical bob so the die reads as shaking in the cup
    // while it rolls, not just spinning in place.
    val bob by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shuffleBob"
    )
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        // cy lifts while the breathe is at rest, so the bob and the pulse
        // rock through a gentle, non-fighting loop.
        val cy = size.height / 2f - bob * r * 0.08f
        val breathe = 1f + pulse * 0.06f
        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            for (i in 0 until 6) {
                val a = (i.toFloat() / 6) * (2f * Math.PI.toFloat())
                drawCircle(
                    color = tint,
                    radius = r * (0.15f * breathe),
                    center = Offset(cx + cos(a) * r * 0.58f, cy + sin(a) * r * 0.58f)
                )
            }
        }
    }
}

/**
 * Small pulsing dot shown on the landed ticket during the opening
 * pause — the subtle heartbeat that says the reveal is about to happen.
 */
@Composable
private fun OpeningPulseDot(tint: Color = Color.White) {
    val infinite = rememberInfiniteTransition(label = "openingPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "openingPulseScale"
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .graphicsLayer {
                scaleX = 1f + pulse * 0.5f
                scaleY = 1f + pulse * 0.5f
                this.alpha = 1f - pulse * 0.4f
            }
            .clip(CircleShape)
            // v7.5 — wears the ticket's ink so the heartbeat reads on the
            // pastel-lightened ticket fill (white when pastel mode is off).
            .background(tint.copy(alpha = 0.9f))
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// Bottom bar — Categories · Filter (solid control buttons)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun BottomCta(
    cat: CurioCategory,
    mixedCount: Int = 1,
    filterActiveCount: Int,
    onCategories: () -> Unit,
    onFilter: () -> Unit,
    vertical: Boolean = false
) {
    val hasFilters = filterActiveCount > 0

    // Anchored paper tray. v6.2 — it wore the SAME category-tint wash as
    // the page background; now transparent so the Categories/Filter buttons
    // sit directly on the Spin page background with no tinted band between
    // them and the nav bar.
    Surface(
        color = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (vertical) {
            // ── Extra-compact edge buttons (v7.2) — on very short screens
            //    the bottom Categories/Filter row becomes two TALL pills
            //    pinned to the left/right screen edges, so the middle stays
            //    clear for the deck band and everything fits.
            // v7.4 — the Scaffold already ends the content area exactly at
            // the app's bottom nav bar (innerPadding.bottom = bar height),
            // so the extra navigationBarsPadding() here pushed the buttons
            // up by the system gesture/3-button inset — the "floating above
            // the nav bar" gap. Removed so the bar sits flush on the nav bar.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                VerticalDeckButton(
                    label = if (mixedCount > 1) "Mixed · $mixedCount" else cat.displayName,
                    icon = cat.iconGlyph,
                    cat = cat,
                    selected = true,
                    onClick = onCategories,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                VerticalDeckButton(
                    label = if (hasFilters) "Filter · $filterActiveCount" else "Filter",
                    icon = CurioIcons.Search,
                    cat = cat,
                    selected = hasFilters,
                    onClick = onFilter,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        } else {
            // v7.4 — same inset fix as the vertical branch above: the
            // content area already ends above the app's bottom nav bar, so
            // navigationBarsPadding() only created a gap under the buttons.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Categories · Filter — image-led deck buttons ────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeckControlButton(
                        // A multi-select deck names itself "Mixed · N" so the
                        // mix is obvious at a glance instead of the first
                        // category's name.
                        label = if (mixedCount > 1) "Mixed · $mixedCount" else cat.displayName,
                        icon = cat.iconGlyph,
                        cat = cat,
                        selected = true,
                        onClick = onCategories,
                        modifier = Modifier.weight(1f)
                    )
                    DeckControlButton(
                        label = if (hasFilters) "Filter · $filterActiveCount" else "Filter",
                        icon = CurioIcons.Search,
                        cat = cat,
                        selected = hasFilters,
                        onClick = onFilter,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Tall vertical pill used by the extra-compact bottom bar (v7.2) — icon
 * over a stacked label, pinned to the left/right screen edge (Categories
 * left, Filter right) so the middle of a very short screen stays clear for
 * the deck band. Same fill/border language as [DeckControlButton].
 */
@Composable
private fun VerticalDeckButton(
    label: String,
    icon: String,
    cat: CurioCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (selected) cat.themedAccent() else cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh),
        border = if (selected) null else cat.categoryBorder(),
        shadowElevation = 0.dp,
        modifier = modifier.size(width = 54.dp, height = 112.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CurioIcon(
                icon, null,
                tint = if (selected) cat.onAccent() else cat.categoryInk(),
                size = 22.dp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = if (selected) cat.onAccent() else cat.categoryInk(),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun DeckControlButton(
    label: String,
    icon: String,
    cat: CurioCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Solid fills — no translucent tint, no border. Selected buttons get the
    // full accent color with white content; unselected buttons get a solid
    // surface fill with theme-aware accent ink (stays readable on the
    // midnight dark surfaces).
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (selected) cat.themedAccent() else cat.categorySurface(MaterialTheme.colorScheme.surfaceContainerHigh),
        border = if (selected) null else cat.categoryBorder(),
        shadowElevation = 0.dp,
        modifier = modifier.height(62.dp)
    ) {
        Row(
            // The icon + label group sits CENTERED in the pill box (not
            // left-flush), so Categories/Filter read as balanced buttons.
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            // spacedBy + CenterHorizontally keeps the icon/text gap while
            // centering the pair as one unit inside the pill.
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            CurioIcon(
                icon, null,
                tint = if (selected) cat.onAccent() else cat.categoryInk(),
                size = 24.dp
            )
            Text(
                text = label,
                // Text-only bump: 14sp → 16sp (icon stays 24dp) so the
                // button labels read a little larger per user request.
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = if (selected) cat.onAccent() else cat.categoryInk(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ═══════════════════════════════════════════════════════════════════════════
// Full-screen category picker dialog — immersive tile grid
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    currentCat: CurioCategory,
    onDismiss: () -> Unit,
    onCategorySelected: (CurioCategory) -> Unit,
    onCategoriesSelected: (List<CurioCategory>) -> Unit,
    onBrowseAll: () -> Unit
) {
    // v7.94 — read the REACTIVE visible list directly (no remember): it
    // recomposes when Manage Categories hides/shows/reorders lanes.
    val categories = CurioCategories.visible
    // Wide windows (tablet / landscape) spread the deck grid and cap the
    // sheet's content width so the picker stays readable on large screens.
    val wide = windowWidthSizeClass().isWide
    // Default = tap-to-open (single). Long-press enters multi-select mode.
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedSlugs by remember { mutableStateOf(setOf<String>()) }

    // Same full-screen + swipe-down-dismiss pattern as the filter page — a
    // ModalBottomSheet expanded to full height with a drag handle.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // v6.6 — the full-screen category selection page wears the
        // same category tint wash as the Spin page it sits on, so
        // the picker never flashes a foreign plain background.
        containerColor = currentCat.categoryBackgroundWash(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        // The sheet spans the whole window; on wide windows the content is
        // centered in the same max-width column as every other page.
        Box(
            modifier = Modifier.fillMaxWidth(),
            // contentAlignment takes a full Alignment, not Alignment.Horizontal
            // (CenterHorizontally) — Center also matches the vertical no-op
            // since the box wraps the sheet content's height.
            contentAlignment = Alignment.Center
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CurioContentMaxWidth)
                .navigationBarsPadding()
        ) {
                    // ── Close button + header ────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            CurioIcon(
                                CurioIcons.Close, "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                size = 22.dp,
                                modifier = Modifier.padding(9.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "What are we exploring?",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        // Current category indicator — or selection count in
                        // multi-select mode.
                        if (multiSelectMode) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = currentCat.themedAccent().copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (selectedSlugs.isEmpty()) "Select decks"
                                    else "${selectedSlugs.size} selected",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = currentCat.categoryInk(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = currentCat.themedAccent().copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = currentCat.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = currentCat.categoryInk(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // ── Mode hint — tap to open, hold to multi-select ──
                    Text(
                        text = if (multiSelectMode) {
                            "Tap to toggle decks · Done to spin together"
                        } else {
                            "Tap a deck to spin it · hold to pick several"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )

                    // ── Tile grid filling the screen ────────────────
                    // v7.4 — the grid sits inside a WEIGHTED Box that is a
                    // DIRECT child of the sheet Column. Weight inside the
                    // old MorphEntrance wrapper was ignored (MorphEntrance's
                    // lambda is not a ColumnScope child), so the grid
                    // rendered at full height and pushed the Mix/Cancel row
                    // off-screen on smaller phones. The Box keeps the
                    // entrance animation AND bounds the grid, so the action
                    // row stays pinned.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        MorphEntrance {
                            LazyVerticalGrid(
                                columns = if (wide) GridCells.Adaptive(minSize = 160.dp) else GridCells.Fixed(2),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                            // All tiles render at once — no per-tile stagger.
                            items(categories) { cat ->
                                val slug = cat.id.routeSlug
                                CurioCategoryCard(
                                    category = cat,
                                    isSelected = if (multiSelectMode) slug in selectedSlugs
                                    else cat.id == currentCat.id,
                                    onClick = {
                                        if (multiSelectMode) {
                                            selectedSlugs = if (slug in selectedSlugs) selectedSlugs - slug
                                            else selectedSlugs + slug
                                        } else {
                                            onCategorySelected(cat)
                                        }
                                    },
                                    onLongClick = {
                                        multiSelectMode = true
                                        if (slug !in selectedSlugs) {
                                            selectedSlugs = selectedSlugs + slug
                                        }
                                    }
                                )
                            }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Browse all link, or Mix row in multi-select ──
                    if (multiSelectMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (selectedSlugs.isEmpty()) return@Button
                                    onCategoriesSelected(
                                        categories.filter { it.id.routeSlug in selectedSlugs }
                                    )
                                },
                                enabled = selectedSlugs.isNotEmpty(),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                CurioIcon(CurioIcons.Check, null, size = 18.dp)
                                Text(
                                    text = if (selectedSlugs.isEmpty()) "Mix" else "Mix · ${selectedSlugs.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            TextButton(
                                onClick = {
                                    multiSelectMode = false
                                    selectedSlugs = emptySet()
                                }
                            ) {
                                Text(
                                    "Cancel",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    } else {
                        TextButton(
                            onClick = onBrowseAll,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            CurioIcon(CurioIcons.Palette, null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Browse all categories",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
        }
    }
    }
}

// Helpers
// ═══════════════════════════════════════════════════════════════════════════

private fun resolveTopicForSlot(
    slot: Int,
    pool: List<CurioTopic>,
    cycleIndex: Int,
    landedTopic: CurioTopic?
): CurioTopic? {
    if (pool.isEmpty()) return null
    if (landedTopic != null && slot == 0) return landedTopic
    // v6.10 — the idle fan and the spinning reel are the SAME window into
    // the hand (front = hand[cycleIndex], neighbors fanned around it), so
    // a spin starts as a seamless +1 continuation — never a jump cut.
    val idxOf = { pos: Int -> ((pos % pool.size) + pool.size) % pool.size }
    return pool[idxOf(cycleIndex + slot)]
}

/**
 * Deals a spin hand — up to 6 topics for the fan. With [center] the landed
 * topic sits at the front (hand[0]) and its neighbors fill the rest; without
 * one the hand is a plain random spread. Stable across a spin: the reel
 * rotates through it via cycleIndex instead of re-shuffling mid-spin.
 *
 * v7.101 — [center] leads the fan even when it's no longer in [pool]: a
 * just-explored topic is excluded from the open deck, but the restored
 * landed card must still sit at the front (tappable, showing its done
 * state) until the next spin — only the NEIGHBORS come from the open pool.
 */
private fun buildDeckHand(pool: List<CurioTopic>, center: CurioTopic?): List<CurioTopic> {
    if (pool.isEmpty()) return emptyList()
    val head = if (center != null) listOf(center) else emptyList()
    val rest = (if (center == null) pool else pool.filterNot { it.id == center.id }).shuffled()
    return (head + rest).take(6)
}

/**
 * Weighted picker — tier bias (tier 1 human-curated marquee first), then
 * tier 2, tier 3, while excluding topics in [recentIds] and any topic the
 * user already explored (captured). Sentiment further skews the weights:
 * liked topics get 2x, disliked drop to 0.25x, and each topic's CATEGORY
 * affinity (net likes − dislikes in that category) boosts or dampens the
 * whole genre — never fully blocked. Falls back gracefully when the pool
 * is all-recent or all-explored.
 *
 * v7.94 — FILMS get a recency nudge: the Films deck is dominated by
 * classics (1940s–1970s), so older films are down-weighted and newer ones
 * boosted. Nothing is removed — a 1950s classic can still land, just less
 * often than a 2020s release.
 *
 * ───────────────────────────────────────────────────────────────────────
 * AGENT NOTE (v7.110): the content fix called for in v7.94 is DONE.
 * `films.json` grew from ~460 to 802 topics via `scripts/batch_films_modern_1.py`
 * (342 modern Hollywood crowd-pleasers, 2000–2025, proper teasers/facts and
 * personalized watch instructions). Decade share is now healthy: 2000s ~90,
 * 2010s ~202, 2020s ~204, so the deck no longer skews pre-1980. The recency
 * factor below is now a gentle tiebreak, not the cure. Future films work:
 * re-run `scripts/batch_films_modern_1.py` (it dedupes by normalized title)
 * or append directly to `films.json` and validate with
 * `python3 scripts/validate_topics.py`.
 * ───────────────────────────────────────────────────────────────────────
 */
private fun pickFrom(
    pool: List<CurioTopic>,
    recentIds: Set<String>,
    exploredIds: Set<String>,
    sentiments: Map<String, String>,
    categoryAffinity: Map<String, Int>
): CurioTopic? {
    if (pool.isEmpty()) return null
    var candidates = pool.filterNot { it.id in recentIds }
    // Explored topics (already captured) are excluded entirely — falling
    // back to the full candidate pool only when everything is explored so
    // the shuffle never runs dry.
    val unvisited = candidates.filterNot { it.id in exploredIds }
    if (unvisited.isNotEmpty()) candidates = unvisited
    if (candidates.isEmpty()) return null
    if (candidates.size == 1) return candidates[0]

    fun baseWeight(t: CurioTopic): Double = when (t.tier) {
        1 -> 100.0
        2 -> 60.0
        3 -> 20.0
        else -> 30.0
    }

    /**
     * v7.94 — Films recency factor. The Films pool leans classic, so a
     * film's decade tag ("1940s" … "2020s") scales its chance: newer films
     * get a boost, pre-1980 classics are down-weighted but never blocked.
     * Topics without a decade tag keep neutral weight.
     */
    val filmDecadeRe = Regex("""^\d{4}s$""")
    fun recencyFactor(t: CurioTopic): Double {
        if (t.categoryId != CategoryId.FILMS) return 1.0
        val year = t.tags.firstNotNullOfOrNull { tag ->
            if (filmDecadeRe.matches(tag)) tag.dropLast(1).toIntOrNull() else null
        } ?: return 1.0
        return when {
            year >= 2020 -> 1.6
            year >= 2010 -> 1.45
            year >= 2000 -> 1.25
            year >= 1990 -> 1.1
            year >= 1980 -> 0.9
            year >= 1970 -> 0.7
            else -> 0.55
        }
    }

    fun weight(t: CurioTopic): Double {
        // Per-topic sentiment: a liked topic gets 2x, a disliked one drops
        // to 0.25x — it can still appear, just far less often.
        val topicFactor = when (sentiments["${t.categoryId.name}:${t.id}"]) {
            AppPreferences.SENTIMENT_LIKE -> 2.0
            AppPreferences.SENTIMENT_DISLIKE -> 0.25
            else -> 1.0
        }
        // Category affinity (net likes − dislikes in the category): a liked
        // genre shows more (up to 2.5x), a disliked genre shows less (down
        // to 0.25x) — never fully blocked.
        val aff = categoryAffinity[t.categoryId.name] ?: 0
        val categoryFactor = when {
            aff > 0 -> 1.0 + 0.5 * aff.coerceAtMost(3)
            aff < 0 -> (1.0 + 0.4 * aff).coerceAtLeast(0.25)
            else -> 1.0
        }
        return baseWeight(t) * topicFactor * categoryFactor * recencyFactor(t)
    }

    val totalWeight = candidates.sumOf { weight(it) }
    if (totalWeight <= 0.0) return candidates.random()
    var target = Random.nextDouble(totalWeight)
    for (topic in candidates) {
        target -= weight(topic)
        if (target < 0) return topic
    }
    return candidates.random()
}
