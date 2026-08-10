package com.curio.app.features.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CurioCategories
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToTab
import com.curio.app.ui.adaptive.CurioContentMaxWidth
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioBackButton
import com.curio.app.ui.components.CurioCategoryCard
import com.curio.app.ui.components.MorphEntrance
import com.curio.app.ui.components.categoryEdgeShine
import com.curio.app.ui.components.curioButtonColors
import com.curio.app.ui.pet.PetLandmarks
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.categoryBackgroundWash

/**
 * Full-screen Category Picker.
 *
 * Default (single-select): **tap a card to open that category in the Spin
 * page immediately** — like the original picker. **Tap and hold** a card to
 * enter multi-select mode; in that mode taps toggle selection (any number)
 * and a **Done** button appears (only then) to launch the Shuffle across
 * every chosen deck. Cards carry a topic count so as many decks as possible
 * fit on screen at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerScreen(navController: NavController) {
    val context = LocalContext.current
    // v7.94 — read the REACTIVE visible list directly (no remember): hidden
    // lanes drop out and reordered lanes follow Manage Categories instantly.
    val categories = CurioCategories.visible
    val gridState = rememberLazyGridState()
    // Wide windows (tablet / landscape) spread the deck grid and cap the
    // sheet's content width so the picker stays readable on large screens.
    val wide = windowWidthSizeClass().isWide
    // ── Category tint wash — this picker hands off straight to the Shuffle
    //    tab, so it wears the last-used deck's color story (same wash as the
    //    Spin page / Save / Cabinet) instead of a plain theme background.
    val washCat = remember {
        val id = AppPreferences.getLastSpinCategories(context).firstOrNull()
            ?: AppPreferences.getLastSpinCategory(context)
        CurioCategories.byId(id)
    }
    // Null = not in multi-select mode (tap-to-open). Once set, cards toggle.
    var selectedSlugs by rememberSaveable { mutableStateOf(listOf<String>()) }
    var multiSelectMode by rememberSaveable { mutableStateOf(false) }

    val toggleSlug = { slug: String ->
        selectedSlugs = if (slug in selectedSlugs) selectedSlugs - slug else selectedSlugs + slug
    }

    // Same full-screen + swipe-down-dismiss pattern as the filter page — a
    // ModalBottomSheet expanded to full height with a drag handle.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // v8.21 — tell the pet a drawer is up so it comes over to peek.
    LaunchedEffect(Unit) { PetLandmarks.noteSheet("picker", true) }
    DisposableEffect(Unit) {
        onDispose { PetLandmarks.noteSheet("picker", false) }
    }

    ModalBottomSheet(
        onDismissRequest = { navController.popBackStack() },
        sheetState = sheetState,
        // Theme-aware category wash — deep accent over cream in light,
        // pastel twin glow over midnight in dark.
        containerColor = washCat.categoryBackgroundWash(),
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
                .padding(horizontal = 16.dp)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CurioBackButton(onClick = { navController.popBackStack() })
            Text(
                text = "What are we exploring?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = if (multiSelectMode) {
                "Tap to pick decks, then Mix to spin them together"
            } else {
                "Tap a deck to spin it, hold one to mix a few"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )

        // v7.4 — the grid sits inside a WEIGHTED Box that is a DIRECT child
        // of the sheet Column. Weight inside the old MorphEntrance wrapper
        // was ignored, so the grid rendered at full height and pushed the
        // Mix row off-screen on smaller phones. The Box keeps the entrance
        // animation AND bounds the grid, so the action row stays pinned.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            MorphEntrance {
                LazyVerticalGrid(
                    state = gridState,
                    columns = if (wide) GridCells.Adaptive(minSize = 160.dp) else GridCells.Fixed(2),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                items(categories) { cat ->
                    val slug = cat.id.routeSlug
                    CurioCategoryCard(
                        category = cat,
                        isSelected = multiSelectMode && slug in selectedSlugs,
                        onClick = {
                            if (multiSelectMode) {
                                toggleSlug(slug)
                            } else {
                                // Default: tap opens this category on the
                                // persistent Shuffle tab (the plain "spin"
                                // route), not a separate spin/{slug} page.
                                // The selection is persisted so it survives
                                // back navigation, tab switches and relaunch.
                                AppPreferences.setLastSpinCategories(context, listOf(cat.id))
                                navController.navigateToTab(CurioRoutes.SPIN)
                            }
                        },
                        onLongClick = {
                            // Enter multi-select mode and select this card.
                            multiSelectMode = true
                            if (slug !in selectedSlugs) toggleSlug(slug)
                        }
                    )
                }
                }
            }
        }

        if (multiSelectMode) {
            // ── Mix row — only visible in multi-select mode ────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val mixShape = RoundedCornerShape(24.dp)
                Button(
                    onClick = {
                        if (selectedSlugs.isEmpty()) return@Button
                        // Resolve the chosen slugs and persist the FULL set
                        // (single or mixed) so the Shuffle tab reopens the
                        // same deck after back navigation, tab switches and
                        // app restarts. navigateToTab drops the picker and
                        // lands on the real Shuffle tab — not a separate
                        // spin/{slug} instance.
                        val ids = selectedSlugs.mapNotNull { CurioCategories.byRouteSlug(it)?.id }
                        if (ids.isEmpty()) return@Button
                        AppPreferences.setLastSpinCategories(context, ids)
                        navController.navigateToTab(CurioRoutes.SPIN)
                    },
                    enabled = selectedSlugs.isNotEmpty(),
                    shape = mixShape,
                    colors = curioButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        // v12 — AMOLED: curioButtonColors forces the plate to
                        // pitch black, so the scheme's onPrimary (a deep
                        // maroon) would vanish on it — the content flips to
                        // white on the black glass.
                        contentColor = if (AppPreferences.themeStyleState == AppPreferences.THEME_STYLE_AMOLED)
                            MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .categoryEdgeShine(mixShape)
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
                        // Exit multi-select mode; selection is discarded.
                        multiSelectMode = false
                        selectedSlugs = emptyList()
                    }
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
    }
    }
}
