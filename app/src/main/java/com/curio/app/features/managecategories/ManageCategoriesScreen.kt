package com.curio.app.features.managecategories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioQuests
import com.curio.app.data.LevelRewards
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsNavRail
import com.curio.app.features.settings.SettingsOptionCard
import com.curio.app.features.settings.SettingsOptionDivider
import com.curio.app.features.settings.SettingsSectionHeading
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.features.settings.navigateToSettingsSection
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryInk
import com.curio.app.ui.theme.isCurioDarkTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.curio.app.features.settings.SettingsHeroTotalHeight

/** Plain float holder for drag geometry — deliberately NOT Compose state so
 *  writes from layout callbacks never recompose the list mid-drag. */
private class WindowPosRef { var value = 0f }

/**
 * Manage Categories — see Curio category-management contract.
 *
 * The settings-family torn-rose hero (shared `SettingsHeroHeader`) on a
 * watermark backdrop, with FLAT category rows (no card shells — icon chip,
 * name + Hidden status, reorder steppers + drag handle, visibility switch)
 * that scroll under the ragged tear.
 *
 * v7.94 — the screen now works FOR REAL: order + visibility are persisted
 * to [AppPreferences] (reactive state), and [CurioCategories.visible]
 * consumes them everywhere — Home/Cabinet chip rows, the Category Picker,
 * and the Spin category sheet all drop hidden lanes and honor the reorder
 * instantly and across restarts. The screen shows ALL categories (hidden
 * ones included, flagged) so nothing can get permanently lost.
 */

@Composable
fun ManageCategoriesScreen(navController: NavController) {
    val context = LocalContext.current
    // The full ordered list — the persisted order (falling back to the
    // default), with every category shown so hidden lanes stay restorable.
    // Reactive: recomposes the instant order/hidden change elsewhere.
    val items: List<CurioCategory> = remember(
        AppPreferences.categoryOrderState,
        AppPreferences.hiddenCategoriesState
    ) {
        val order = AppPreferences.categoryOrderState
        val base = if (order.isEmpty()) {
            CurioCategories.all
        } else {
            order.mapNotNull { id -> CurioCategories.all.firstOrNull { it.id == id } } +
                CurioCategories.all.filter { it.id !in order }
        }
        base.map { cat -> cat.copy(isHidden = cat.id in AppPreferences.hiddenCategoriesState) }
    }
    // v26 — local DRAFT order while the user drags: steppers and the
    // long-press drag mutate the draft and persist on release. Re-keyed off
    // [items] so external changes (hidden toggles) re-seed it cleanly.
    var draft by remember(items) { mutableStateOf(items) }
    // v3xx — REAL drag-reorder: the WHOLE row is the drag surface (long-press
    // anywhere, so the list scroll can't steal the gesture), the dragged row
    // follows the finger exactly, neighbours shift with springs as slots are
    // crossed, and the list AUTO-SCROLLS when the finger reaches its edges.
    var draggingId by remember { mutableStateOf<CategoryId?>(null) }
    // The dragged row's finger-follow travel (px, viewport space).
    var dragTravelY by remember { mutableFloatStateOf(0f) }
    // Auto-scroll applied so far during the drag (px, content space) — feeds
    // the slot math so a stationary finger at the edge still swaps lanes as
    // the list scrolls under it.
    var scrollAccum by remember { mutableFloatStateOf(0f) }
    // The finger's y inside the LIST VIEWPORT (px) — read by the auto-scroll
    // loop; Float.MAX_VALUE when idle (no drag → no auto-scroll).
    var dragFingerY by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    // Average row height (the 56dp reorder column + 20dp padding dominates).
    val rowHeightPx = with(LocalDensity.current) { 76.dp.toPx() }
    // Auto-scroll edge zone + top speed (px per 16ms frame).
    val edgeZonePx = with(LocalDensity.current) { 120.dp.toPx() }
    val autoScrollMaxPx = 10f
    // v5.8 — saveable-backed: keep the list's scroll position on rotation.
    val listState = rememberLazyListState()
val glassBackdrop = rememberLayerBackdrop()
    // ── Drag geometry, held WITHOUT recomposition (plain holders): the list
    //    viewport top/height and every row's window top. Read only inside the
    //    drag callbacks / auto-scroll loop, so layout changes never recompose
    //    the whole list mid-drag.
    val listTopRef = remember { WindowPosRef() }
    val viewportHRef = remember { WindowPosRef() }
    val rowTops = remember { mutableMapOf<CategoryId, Float>() }

    // v3xx — AUTO-SCROLL while dragging: while a row is held, scroll the
    // list when the finger is inside the top/bottom edge zones, with speed
    // proportional to how deep the finger is in the zone. The scrolled
    // distance accumulates into [scrollAccum] so the slot math follows.
    LaunchedEffect(draggingId) {
        if (draggingId == null) return@LaunchedEffect
        while (true) {
            val finger = dragFingerY
            val viewportH = viewportHRef.value
            val delta = when {
                finger < edgeZonePx ->
                    -((edgeZonePx - finger) / edgeZonePx) * autoScrollMaxPx
                viewportH > 0f && finger > viewportH - edgeZonePx ->
                    ((finger - (viewportH - edgeZonePx)) / edgeZonePx) * autoScrollMaxPx
                else -> 0f
            }
            if (delta != 0f) {
                val scrolled = listState.scrollBy(delta)
                if (scrolled != 0f) scrollAccum += delta
            }
            delay(16)
        }
    }
    // v-tablet — the torn hero is NOT sticky on wide windows (landscape
    // tablet): it leads the list as its first item and scrolls away with it;
    // the pinned glass overlay stays phone-only.
    val wide = windowWidthSizeClass().isWide

    fun shiftDraft(id: CategoryId, delta: Int) {
        val idx = draft.indexOfFirst { it.id == id }
        if (idx < 0) return
        val target = (idx + delta).coerceIn(0, draft.lastIndex)
        if (target == idx) return
        draft = draft.toMutableList().apply { add(target, removeAt(idx)) }
    }

    fun persistDraft() {
        AppPreferences.setCategoryOrder(context, draft.map { it.id })
    }

    // v9.x — custom lane order is a LEVEL REWARD: the drag-reorder + steppers
    // stay locked until the player reaches the lane-order milestone, so XP
    // has a concrete payoff. Hiding lanes stays open to everyone.
    val reorderLevel = LevelRewards.laneOrderReward?.level ?: 5
    val reorderUnlocked = CurioQuests.levelForXp(CurioQuests.xpState) >= reorderLevel

    // The hero banner runs up BEHIND the status bar (the shared header
    // applies its own status-bar inset for the back pill) — the settings
    // family construction, so the page tears from the very top edge. The
    // hero is drawn LAST (on top of the scroll content): the rows scroll
    // UP and disappear behind the ragged tear instead of clipping at a
    // straight line.
    Box(
        modifier = Modifier
            .fillMaxSize()
            // v30 — "Hero follows Spin lane": the page wears the lane wash.
            .background(heroPageBackground())
    ) {
        // ── Watermark backdrop — muted category glyphs behind the flat
        //    rows (the settings-family quieted whisper, so text reads).
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD),
                alphaScale = 0.45f
            )
        }

        ScreenEntrance {
            LazyColumn(
                state = listState,
                // v142 — full-bleed bottom: the NavHost no longer reserves
                // the nav-bar slot for this route, so the page clears the
                // gesture bar itself (the wash runs to the bottom edge).
                modifier = Modifier
                    // v3xx — the auto-scroll loop reads the list viewport's
                    // window top + height (non-state holders, no recompose).
                    .onGloballyPositioned { coords ->
                        listTopRef.value = coords.positionInWindow().y
                        viewportHRef.value = coords.size.height.toFloat()
                    }
                    .layerBackdrop(glassBackdrop)
                    .fillMaxSize()
                    .navigationBarsPadding(),                    contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    // v255 — SCROLLING HERO: the banner is the list's first
                    // item and scrolls away with the page.
                    top = if (wide) 0.dp else SettingsHeroTotalHeight,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
            if (wide) {
                item(key = "hero", contentType = "hero") {
                    SettingsHeroHeader(
                        title = "Manage categories",
                        subtitle = "Show, hide, or reorder lanes",
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            // v3xx — the shared settings nav rail: switch sections without
            // going back to the hub (the open page sits in the 2nd slot).
            item(key = "settings-nav", contentType = "settings-nav") {
                SettingsNavRail(
                    active = "categories",
                    onSelect = { navigateToSettingsSection(navController, it) },
                    navController = navController
                )
            }
                                // v9.x — locked-reorder notice: explains the level gate and
                // shows how far away it is when the player hasn't unlocked it.
                if (!reorderUnlocked) {
                    item("reorder-lock") {
                        val remaining = reorderLevel - CurioQuests.levelForXp(CurioQuests.xpState)
                        val lockDark = isCurioDarkTheme()
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (lockDark) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
                            else Color.White.copy(alpha = 0.68f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Frosted icon tile — the settings row language.
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(
                                            if (lockDark) Color.White.copy(alpha = 0.09f)
                                            else Color(0xFFF2E8DC)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CurioIcon(
                                        name = CurioIcons.DragHandle,
                                        contentDescription = null,
                                        tint = if (lockDark) Color(0xFFD7B8A9) else Color(0xFF755647),
                                        size = 20.dp
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Custom order locked",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Reach Level $reorderLevel to reorder your lanes" +
                                            " ($remaining level${if (remaining == 1) "" else "s"} to go). Hiding still works.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Helper text + Reset order — flat caption under the hero
                item("help") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hidden categories won't show in Shuffle, Category Picker, or Cabinet. " +
                                  "Past entries in hidden categories are kept and reappear when you re-enable them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                // v26 — restore the default lane order.
                                draggingId = null
                                dragTravelY = 0f
                                scrollAccum = 0f
                                dragFingerY = Float.MAX_VALUE
                                AppPreferences.setCategoryOrder(
                                    context, CurioCategories.all.map { it.id }
                                )
                            }
                        ) {
                            Text("Reset order")
                        }
                    }
                }

                // ── Section heading + the frosted lanes card — the settings
                //    option-card language (one card, hairline-divided rows,
                //    exactly like the Recording page). ──
                item("lanes-heading") {
                    SettingsSectionHeading("Your lanes", "\u2726")
                }
                item("lanes") {
                    SettingsOptionCard {
                        // ── Drag-reorder math (v3xx): while a row is held the
                        //    draft is FROZEN (no mutation until release) — the
                        //    dragged row follows the finger exactly, neighbours
                        //    shift one slot with springs as the target moves,
                        //    and on release the order is committed ONCE and
                        //    every row springs to its final slot (the buttery
                        //    settle instead of a snap).
                        val origIndex = draft.indexOfFirst { it.id == draggingId }
                        val lastIndex = draft.lastIndex
                        val dragActive = draggingId != null
                        val effectiveTravel = if (dragActive) dragTravelY + scrollAccum else 0f
                        val targetIndex = if (dragActive)
                            (origIndex + (effectiveTravel / rowHeightPx).roundToInt()).coerceIn(0, lastIndex)
                        else origIndex
                        draft.forEachIndexed { index, category ->
                            if (index > 0) SettingsOptionDivider()
                            val isDragged = category.id == draggingId
                            // The placeholder shift: rows between the dragged
                            // row's ORIGINAL slot and its current TARGET slot
                            // shift one slot out of the way (±rowHeight).
                            val shiftBy = when {
                                !dragActive || isDragged -> 0f
                                targetIndex > origIndex && index in (origIndex + 1)..targetIndex -> -rowHeightPx
                                targetIndex < origIndex && index in targetIndex until origIndex -> rowHeightPx
                                else -> 0f
                            }
                            key(category.id) {
                                val offsetAnim = remember { Animatable(0f) }
                                val desired = if (isDragged) dragTravelY else shiftBy
                                LaunchedEffect(desired, isDragged) {
                                    if (isDragged) offsetAnim.snapTo(desired)
                                    else offsetAnim.animateTo(
                                        desired,
                                        animationSpec = spring(dampingRatio = 0.95f, stiffness = 500f)
                                    )
                                }
                                CategoryRow(
                                    category = category,
                                    isFirst = draft.firstOrNull()?.id == category.id,
                                    isLast = draft.lastOrNull()?.id == category.id,
                                    isDragging = isDragged,
                                    reorderEnabled = reorderUnlocked,
                                    dragOffsetY = offsetAnim.value,
                                    onMoveUp = { if (reorderUnlocked) { shiftDraft(category.id, -1); persistDraft() } },
                                    onMoveDown = { if (reorderUnlocked) { shiftDraft(category.id, +1); persistDraft() } },
                                    onDragStart = { offset ->
                                        draggingId = category.id
                                        dragTravelY = 0f
                                        scrollAccum = 0f
                                        dragFingerY = (rowTops[category.id] ?: listTopRef.value) +
                                            offset.y - listTopRef.value
                                    },
                                    onDrag = { change, dy ->
                                        dragTravelY += dy
                                        // The finger's VIEWPORT position = its
                                        // position inside the row + the row's
                                        // window top, minus the list top. Auto-
                                        // scroll moves the CONTENT, not the
                                        // finger, so this stays correct as the
                                        // list scrolls under the drag.
                                        dragFingerY = (rowTops[category.id] ?: listTopRef.value) +
                                            change.position.y - listTopRef.value
                                    },
                                    onDragEnd = {
                                        val id = draggingId
                                        if (id != null) {
                                            val from = draft.indexOfFirst { it.id == id }
                                            if (from >= 0) {
                                                val to = (from + ((dragTravelY + scrollAccum) / rowHeightPx).roundToInt())
                                                    .coerceIn(0, draft.lastIndex)
                                                if (to != from) {
                                                    draft = draft.toMutableList().apply { add(to, removeAt(from)) }
                                                    persistDraft()
                                                }
                                            }
                                        }
                                        draggingId = null
                                        dragTravelY = 0f
                                        scrollAccum = 0f
                                        dragFingerY = Float.MAX_VALUE
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragTravelY = 0f
                                        scrollAccum = 0f
                                        dragFingerY = Float.MAX_VALUE
                                    },
                                    onVisibilityToggle = { visible ->
                                        // Persist instantly — the app-wide reactive state
                                        // updates and every consumer recomposes.
                                        AppPreferences.setCategoryHidden(context, category.id, !visible)
                                    },
                                    // v3xx — the WHOLE row is the drag surface
                                    // (long-press anywhere; taps still reach the
                                    // switch/steppers). The long-press gesture
                                    // lives INSIDE CategoryRow — here we only
                                    // track each row's window top so the drag
                                    // math has accurate finger/row positions.
                                    modifier = Modifier.then(
                                        if (reorderUnlocked)
                                            Modifier.onGloballyPositioned { coords ->
                                                rowTops[category.id] = coords.positionInWindow().y
                                            }
                                        else Modifier
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Side scroll indicator — thin overlay knob, grows on touch.
        CurioVerticalScrollIndicator(
            state = listState.scrollIndicatorState,
            onScrollBy = { listState.dispatchRawDelta(it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 16.dp)
        )
                // RESTORED (user request) — STICKY HERO drawn on TOP of the scroll
        // content: rows slide under the ragged tear as they scroll up, and
        // the back pill refracts them through REAL liquid glass.
        // v-tablet — pinned overlay is phone-only; wide windows scroll the
        // hero as the list's first item instead.
        if (!wide) {
            SettingsHeroHeader(title = "Manage categories", subtitle = "Show, hide, or reorder lanes", onBack = { navController.popBackStack() }, glassBackdrop = glassBackdrop)
        }

    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CategoryRow(
    category: CurioCategory,
    isFirst: Boolean,
    isLast: Boolean,
    isDragging: Boolean = false,
    reorderEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    // v3xx — the row's animated drag/placeholder offset (px), driven by the
    // card's per-row Animatable so the dragged row follows the finger and
    // neighbours glide as slots are crossed.
    dragOffsetY: Float = 0f,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (PointerInputChange, Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onVisibilityToggle: (Boolean) -> Unit
) {
    val hiddenAlpha by animateFloatAsState(
        targetValue = if (category.isHidden) 0.45f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "hiddenAlpha"
    )
    // v3xx40 — while DRAGGING the row must stay clearly visible: the flat
    // row disappears against the neighbours it slides over, so the dragged
    // row swaps to a LIFTED CARD look — an opaque surface, hairline primary
    // outline, soft shadow and FULL alpha (the hidden fade is suppressed
    // for the dragged row). Neighbours keep the flat look.
    // (Shadow BEFORE the fill per the shadow-order rule.)
    val rowAlpha = if (isDragging) 1f else hiddenAlpha
    val draggedShell = if (isDragging)
        Modifier
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCurioDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHighest
                else Color(0xFFF7F1E6)
            )
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
    else Modifier

    // Flat row — no card shell: a tinted icon chip, the name + Hidden
    // status, the reorder steppers + drag handle, and the visibility
    // switch, sitting directly on the watermark backdrop. While dragging
    // the row lifts (zIndex + slight scale + the card shell) above its
    // neighbors.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = if (isDragging) 1.03f else 1f
                scaleY = if (isDragging) 1.03f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f)
            // v3xx — the finger-follow / placeholder offset (layout-space so
            // the row physically moves; zIndex keeps the dragged row above).
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .then(draggedShell)
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .alpha(rowAlpha)
            // v3xx — the whole row is the long-press drag surface: the list
            // scroll can never steal the gesture once the long press lands.
            .then(
                if (reorderEnabled)
                    Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = onDragStart,
                            onDrag = { change, amount ->
                                change.consume()
                                onDrag(change, amount.y)
                            },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel
                        )
                    }
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Reorder stepper + drag handle (v26: long-press to drag; the
        //    pointer input itself now lives on the WHOLE row — v3xx — so
        //    this column is just the visual affordance) ──
        // v9.x — the whole reorder column is level-gated: locked players see
        // a dimmed lock icon instead of the handle, so the payoff is visible.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.size(width = 40.dp, height = 56.dp)
        ) {
            if (reorderEnabled) {
                ReorderButton(
                    glyph = CurioIcons.KeyboardArrowUp,
                    enabled = !isFirst,
                    onClick = onMoveUp
                )
            }
            CurioIcon(
                name = CurioIcons.DragHandle,
                contentDescription = if (reorderEnabled) "Drag to reorder" else "Locked · reach the lane-order level",
                tint = if (isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (reorderEnabled) 1f else 0.45f)
                },
                size = 20.dp
            )
            if (reorderEnabled) {
                ReorderButton(
                    glyph = CurioIcons.KeyboardArrowDown,
                    enabled = !isLast,
                    onClick = onMoveDown
                )
            }
        }

        // ── Category icon chip — tinted rounded square (the drawer's icon
        //    chip language), icon in the category's readable ink ─────────
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = category.tint.copy(alpha = 0.16f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CurioIcon(
                    name = category.iconGlyph,
                    contentDescription = null,
                    tint = category.categoryInk(),
                    size = 22.dp
                )
            }
        }

        // ── Name + status ──────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            AnimatedVisibility(visible = category.isHidden) {
                Text(
                    text = "Hidden",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Visibility toggle ───────────────────────────────────────────
        Switch(
            checked = !category.isHidden,
            onCheckedChange = { newVisible -> onVisibilityToggle(newVisible) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun ReorderButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        modifier = Modifier.size(20.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CurioIcon(
                name = glyph,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                },
                size = 16.dp
            )
        }
    }
}
