package com.curio.app.features.managecategories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioSettingsDivider
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryInk

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
    // long-press drag mutate the draft and persist on release, so the list
    // animates into place (animateItem) instead of jumping. Re-keyed off
    // [items] so external changes (hidden toggles) re-seed it cleanly.
    var draft by remember(items) { mutableStateOf(items) }
    var draggingId by remember { mutableStateOf<CategoryId?>(null) }
    // Residual drag travel (px) — swaps fire when a full row step accrues.
    var dragAccum by remember { mutableFloatStateOf(0f) }
    // Average row height (the 56dp reorder column + 20dp padding dominates).
    val dragStepPx = with(LocalDensity.current) { 76.dp.toPx() }
    // v5.8 — saveable-backed: keep the list's scroll position on rotation.
    val listState = rememberLazyListState()

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
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    // v255 — SCROLLING HERO: the banner is the list's first
                    // item and scrolls away with the page.
                    top = 10.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item("hero") {
                    SettingsHeroHeader(
                        title = "Manage categories",
                        subtitle = "Show, hide, or reorder lanes",
                        onBack = { navController.popBackStack() }
                    )
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
                                dragAccum = 0f
                                AppPreferences.setCategoryOrder(
                                    context, CurioCategories.all.map { it.id }
                                )
                            }
                        ) {
                            Text("Reset order")
                        }
                    }
                }

                // ── Category rows — flat, with hairlines between them ───
                // Draggable: long-press the ⋮ handle, then drag up/down; the
                // lane glides into place and the order persists on release.
                itemsIndexed(draft, key = { _, category -> category.id }) { index, category ->
                    CategoryRow(
                        category = category,
                        isFirst = draft.firstOrNull()?.id == category.id,
                        isLast = draft.lastOrNull()?.id == category.id,
                        isDragging = draggingId == category.id,
                        modifier = Modifier.animateItem(),
                        onMoveUp = { shiftDraft(category.id, -1); persistDraft() },
                        onMoveDown = { shiftDraft(category.id, +1); persistDraft() },
                        onDragStart = {
                            draggingId = category.id
                            dragAccum = 0f
                        },
                        onDragDelta = { dy ->
                            dragAccum += dy
                            // A full row-height of travel swaps the lane one
                            // slot; the residual carries into the next swap
                            // so long fast drags feel continuous.
                            while (dragAccum >= dragStepPx) {
                                dragAccum -= dragStepPx
                                shiftDraft(category.id, +1)
                            }
                            while (dragAccum <= -dragStepPx) {
                                dragAccum += dragStepPx
                                shiftDraft(category.id, -1)
                            }
                        },
                        onDragEnd = {
                            draggingId = null
                            dragAccum = 0f
                            persistDraft()
                        },
                        onDragCancel = {
                            draggingId = null
                            dragAccum = 0f
                        },
                        onVisibilityToggle = { visible ->
                            // Persist instantly — the app-wide reactive state
                            // updates and every consumer recomposes.
                            AppPreferences.setCategoryHidden(context, category.id, !visible)
                        }
                    )
                    // Hairline between rows — the flat-list divider language.
                    if (index < draft.lastIndex) {
                        CurioSettingsDivider()
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
    }
}

@Composable
private fun CategoryRow(
    category: CurioCategory,
    isFirst: Boolean,
    isLast: Boolean,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onVisibilityToggle: (Boolean) -> Unit
) {
    val hiddenAlpha by animateFloatAsState(
        targetValue = if (category.isHidden) 0.45f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "hiddenAlpha"
    )

    // Flat row — no card shell: a tinted icon chip, the name + Hidden
    // status, the reorder steppers + drag handle, and the visibility
    // switch, sitting directly on the watermark backdrop. While dragging
    // the row lifts (zIndex + slight scale) above its neighbors.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = if (isDragging) 1.03f else 1f
                scaleY = if (isDragging) 1.03f else 1f
                alpha = if (isDragging) 0.9f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .alpha(hiddenAlpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Reorder stepper + real drag handle (v26: long-press to drag) ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .size(width = 40.dp, height = 56.dp)
                .pointerInput(category.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDrag = { change, amount ->
                            change.consume()
                            onDragDelta(amount.y)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragCancel() }
                    )
                }
        ) {
            ReorderButton(
                glyph = CurioIcons.KeyboardArrowUp,
                enabled = !isFirst,
                onClick = onMoveUp
            )
            CurioIcon(
                name = CurioIcons.DragHandle,
                contentDescription = "Drag to reorder",
                tint = if (isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                size = 20.dp
            )
            ReorderButton(
                glyph = CurioIcons.KeyboardArrowDown,
                enabled = !isLast,
                onClick = onMoveDown
            )
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
