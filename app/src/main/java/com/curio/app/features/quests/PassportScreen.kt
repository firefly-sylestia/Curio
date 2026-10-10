package com.curio.app.features.quests

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioPassport
import com.curio.app.navigation.CurioRoutes
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.features.settings.heroPageBackground
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.components.CurioCardHeader
import com.curio.app.ui.components.CurioForwardArrow
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioVerticalScrollIndicator
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.components.curioPressClickable
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.CurioMotion
import com.curio.app.ui.theme.curioSageInk
import com.curio.app.ui.theme.readableAccentInk
import com.curio.app.ui.theme.themedAccent
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * v498 — THE PASSPORT HAS A PAGE OF ITS OWN.
 *
 * The member, asked on the v497 report whether the passport should become a real
 * screen: **"Build a real passport screen"**. It is the agenda's own §5.4 —
 * *"the Passport as an object"* — and the reason it was owed is that a passport is
 * the one surface in Curio that is a THING rather than a list: `CurioPassport` is
 * a full model (four states a lane can be in, a map of counters, a pressed-stamp
 * record) and until now every bit of it was read out through a 4×3 grid inside a
 * card on the Quests page, where a mastered lane is a small tile among twelve.
 *
 * So the screen is the object:
 *
 *  · **The cover** — how many lanes are mastered, explored and still new, with the
 *    mastery bar, and (when the stamp is on) how many masteries are still waiting
 *    for their ink. One card, no prose.
 *  · **The pages** — every visible lane as a proper stamp, four across in a grid
 *    that scrolls, in the passport's own order (the lane's own id), so the same
 *    lane is always in the same place on the same page.
 *  · **The press** — the v491 ceremony, unchanged and now also PERSISTED (v497
 *    fixed that: a press writes `CurioPassport.markStampPressed`): a mastered lane
 *    that has not been inked says **Press to stamp**, the thumb squashes the
 *    paper, and the imprint lands wide with a thunk and settles crooked. Every
 *    inked stamp is crooked in a way fixed for that lane for life; an unseen lane
 *    is an empty slot and stays square.
 *  · **And every other tap** — a lane that is not waiting for its stamp opens that
 *    lane's Spin deck, exactly as the Quests card's stamps always did, so nothing
 *    on the old card was taken away by moving it here.
 */
@Composable
fun PassportScreen(navController: NavController) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val glassBackdrop = rememberLayerBackdrop()
    val wide = windowWidthSizeClass().isWide

    val cats = CurioCategories.visible
    val stampPress = AppPreferences.passportStampState
    // The pressed record is STORE state (v497): read once on entry and then moved
    // locally as the member lands a stamp, so a cell re-inks on its own frame.
    var pressedLanes by remember(stampPress) { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(stampPress) {
        pressedLanes = if (stampPress) CurioPassport.pressedStamps(context) else emptySet()
    }
    // ONE read of the counters for the whole page, not one per cell: the grid
    // reads every lane's progress for the cover's numbers anyway.
    val progress = remember(context) { CurioPassport.allProgress(context) }
    val mastered = cats.count { progress[it.id]?.stamp == CurioPassport.Stamp.MASTERED }
    val explored = cats.count {
        progress[it.id]?.stamp == CurioPassport.Stamp.MASTERED ||
            progress[it.id]?.stamp == CurioPassport.Stamp.EXPLORED
    }
    val awaitingStamp = if (!stampPress) 0 else cats.count {
        progress[it.id]?.stamp == CurioPassport.Stamp.MASTERED && it.id.name !in pressedLanes
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(heroPageBackground())
    ) {
        // The same collage language as Quests — the passport is lane-neutral, so
        // the wildcard sparkle leads it.
        if (!wide) {
            CurioWatermarkBackdrop(activeCat = CurioCategories.byId(CategoryId.WILDCARD))
        }
        ScreenEntrance {
            LazyColumn(
                state = listState,
                modifier = Modifier.layerBackdrop(glassBackdrop).fillMaxSize(),
                contentPadding = PaddingValues(
                    start = wideContentEdgePadding(),
                    end = wideContentEdgePadding(),
                    top = if (wide) 0.dp else SettingsHeroTotalHeight,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (wide) {
                    item(key = "hero", contentType = "hero") {
                        SettingsHeroHeader(
                            title = "Passport",
                            subtitle = "Every lane you have explored, stamped",
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                item("cover") {
                    PassportCover(
                        mastered = mastered,
                        explored = explored,
                        total = cats.size,
                        awaitingStamp = awaitingStamp
                    )
                }
                item("pages") {
                    PassportBook(
                        cats = cats,
                        progress = progress,
                        pressed = pressedLanes,
                        stampPressOn = stampPress,
                        onStampPress = { cat ->
                            CurioPassport.markStampPressed(context, cat.id)
                            pressedLanes = pressedLanes + cat.id.name
                        },
                        onSpin = { cat ->
                            navController.navigate(
                                CurioRoutes.spinWithCategory(cat.id.routeSlug)
                            ) { launchSingleTop = true }
                        }
                    )
                }
            }
        }
        CurioVerticalScrollIndicator(
            state = listState.scrollIndicatorState,
            onScrollBy = { listState.dispatchRawDelta(it) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 10.dp, bottom = 16.dp)
        )
        if (!wide) {
            SettingsHeroHeader(
                title = "Passport",
                subtitle = "Every lane you have explored, stamped",
                onBack = { navController.popBackStack() },
                glassBackdrop = glassBackdrop
            )
        }
    }
}

/**
 * THE COVER — the passport's own numbers, before the pages.
 *
 * Four facts, three of them a count and one of them a bar. Deliberately no
 * explanation of what a stamp is: the cells below say "Press to stamp" in words,
 * and a line here explaining the passport to its owner would be a label on a
 * label.
 */
@Composable
private fun PassportCover(
    mastered: Int,
    explored: Int,
    total: Int,
    /** Mastered lanes still waiting for their ink — 0 when the press is off. */
    awaitingStamp: Int
) {
    val scheme = MaterialTheme.colorScheme
    val sage = curioSageInk()
    CurioSettingsCard {
        CurioCardHeader(
            CurioIcons.Star,
            "Your passport",
            if (awaitingStamp > 0) "$mastered of $total lanes mastered · $awaitingStamp to stamp"
            else "$mastered of $total lanes mastered"
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else mastered.toFloat() / total.toFloat() },
            color = sage,
            trackColor = lerp(scheme.surfaceContainerLow, scheme.onSurface, 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PassportTally(value = mastered, label = "mastered", tint = sage, modifier = Modifier.weight(1f))
            PassportTally(
                value = explored,
                label = "explored",
                tint = scheme.primary,
                modifier = Modifier.weight(1f)
            )
            PassportTally(
                value = total - explored,
                label = "still new",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** One number of the cover. */
@Composable
private fun PassportTally(value: Int, label: String, tint: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, tint, 0.10f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                "$value",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = tint
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * THE PAGES — every lane as a stamp, four across.
 *
 * The grid is the lane catalog's own order, so a lane keeps its place between
 * visits and between devices (the order is the catalog's, not the activity's).
 * A row's short tail is padded with weighted spacers rather than stretched, so
 * the last stamps line up under the first ones.
 */
@Composable
private fun PassportBook(
    cats: List<CurioCategory>,
    progress: Map<CategoryId, CurioPassport.CategoryProgress>,
    pressed: Set<String>,
    stampPressOn: Boolean,
    onStampPress: (CurioCategory) -> Unit,
    onSpin: (CurioCategory) -> Unit
) {
    CurioSettingsCard {
        CurioCardHeader(
            CurioIcons.Inventory2,
            "The stamps",
            if (stampPressOn) "Tap a lane to open it · press a mastered one to ink it"
            else "Tap a lane to open its deck"
        )
        Spacer(Modifier.height(10.dp))
        cats.chunked(4).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { cat ->
                    PassportStampCell(
                        cat = cat,
                        stamp = progress[cat.id]?.stamp ?: CurioPassport.Stamp.UNSEEN,
                        pressed = cat.id.name in pressed,
                        stampPressOn = stampPressOn,
                        onStampPress = { onStampPress(cat) },
                        onOpen = { onSpin(cat) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ── THE IMPRINT'S IMPERFECTION ────────────────────────────────────────────
// A real stamp never lands square. Every INKED cell carries a small tilt and
// nudge, and both are derived from the LANE'S OWN ID — which makes them stable
// for that lane for life: the same lane is always crooked the same way, a fresh
// install draws the same passport, and nothing here can shimmer or re-roll
// between frames. (`String.hashCode` is specified by the JDK, so these numbers
// survive every future update of the app; a per-frame random would be a bug, not
// a paper texture.)
private fun stampTiltDeg(laneId: String): Float =
    ((((laneId.hashCode() % 25) + 25) % 25) - 12) * 0.2f          // −2.4° … +2.4°

private fun stampNudgeX(laneId: String): Float =
    (((((laneId.hashCode() / 7) % 5) + 5) % 5) - 2) * 0.6f         // −1.2 … +1.2dp

private fun stampNudgeY(laneId: String): Float =
    (((((laneId.hashCode() / 13) % 5) + 5) % 5) - 2) * 0.5f        // −1.0 … +1.0dp

/**
 * ONE STAMP — the lane's glyph, name and state, with the press on it.
 *
 * [stamp] is passed in rather than read here: the whole page reads the counters
 * once (see [PassportScreen]), so a 21-lane grid is one pass over the prefs file
 * and not twenty-one.
 */
@Composable
internal fun PassportStampCell(
    cat: CurioCategory,
    stamp: CurioPassport.Stamp,
    pressed: Boolean,
    stampPressOn: Boolean,
    onStampPress: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val accent = cat.themedAccent()
    val ink = cat.readableAccentInk()

    // EARNED, NOT YET INKED: mastery is a fact derived from real activity and is
    // never in question here — the member simply has not brought the head down.
    val awaiting = stampPressOn && stamp == CurioPassport.Stamp.MASTERED && !pressed
    // The strike is a one-shot: snap the imprint wide, then let one spring settle
    // it into the paper. It ends at rest, so the layer below is a no-op after.
    var landing by remember { mutableStateOf(false) }
    val strike = remember { Animatable(1f) }
    LaunchedEffect(landing) {
        if (landing) {
            strike.snapTo(1.32f)
            strike.animateTo(1f, CurioMotion.Springs.Bouncy)
        }
    }

    val label: String
    val glyph: String
    val tint: Color
    when {
        awaiting -> {
            // The label IS the action: it is the only place the press is named.
            label = "Press to stamp"; glyph = CurioIcons.TaskAlt; tint = curioSageInk()
        }
        else -> when (stamp) {
            CurioPassport.Stamp.MASTERED -> {
                label = "Mastered"; glyph = CurioIcons.TaskAlt; tint = curioSageInk()
            }
            CurioPassport.Stamp.EXPLORED -> {
                label = "Explored"; glyph = CurioIcons.Check; tint = curioSageInk()
            }
            CurioPassport.Stamp.PEEKED -> {
                label = "Peeked"; glyph = CurioIcons.Star; tint = ink
            }
            CurioPassport.Stamp.UNSEEN -> {
                label = "New · spin!"; glyph = CurioIcons.StarOutline; tint = ink
            }
        }
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when (stamp) {
            // OPAQUE fills: a translucent one lets the elevation shadow bleed
            // through (root rule 11).
            CurioPassport.Stamp.MASTERED ->
                lerp(MaterialTheme.colorScheme.surfaceContainerLow, curioSageInk(), 0.12f)
            CurioPassport.Stamp.UNSEEN ->
                lerp(MaterialTheme.colorScheme.surfaceContainerLow, accent, 0.10f)
            else ->
                lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceVariant, 0.45f)
        },
        border = BorderStroke(
            1.dp,
            when {
                awaiting -> curioSageInk().copy(alpha = 0.75f)
                stamp == CurioPassport.Stamp.UNSEEN -> ink.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        ),
        shadowElevation = 2.dp,
        modifier = modifier
            // THE PRESS IS THE GESTURE, and the label is the visible way to do it:
            // nothing here is a hidden gesture or the only route to anything. A
            // waiting cell squashes deeper (a stamp head comes down hard) with the
            // shared tick on the way down; the thunk is on the LAND.
            .curioPressClickable(
                pressedScale = if (awaiting) 0.93f else 0.97f,
                onClickLabel = if (awaiting) "Press the ${cat.displayName} stamp" else null,
                onClick = {
                    if (awaiting) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStampPress()
                        landing = true
                    } else {
                        onOpen()
                    }
                }
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 12.dp)
                .graphicsLayer {
                    // The imperfection lives on the INK, not the card: the cell is
                    // the paper, and the paper is not crooked.
                    if (stampPressOn && stamp != CurioPassport.Stamp.UNSEEN) {
                        rotationZ = stampTiltDeg(cat.id.name)
                        translationX = stampNudgeX(cat.id.name)
                        translationY = stampNudgeY(cat.id.name)
                    }
                    val scale = if (landing) strike.value else 1f
                    scaleX = scale
                    scaleY = scale
                    alpha = if (landing) {
                        (1f - ((strike.value - 1f) / 0.32f)).coerceIn(0f, 1f)
                    } else 1f
                }
        ) {
            CurioIcon(
                name = cat.iconGlyph,
                contentDescription = cat.displayName,
                // Waiting ink is faint: the hue is there, the impression is not.
                tint = if (awaiting) ink.copy(alpha = 0.4f) else ink,
                size = 22.dp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                cat.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = tint,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * THE DOOR ON THE QUESTS PAGE — the passport as one compact row.
 *
 * The grid moved to [PassportScreen], and the Quests card is what it should be on
 * a page about quests: a summary and a way in. Nothing is lost — every stamp, the
 * press and the lane's own spin are on the screen, one tap away.
 */
@Composable
internal fun PassportDoor(
    mastered: Int,
    total: Int,
    awaitingStamp: Int,
    onOpen: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val sage = curioSageInk()
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = lerp(scheme.surfaceContainerLow, sage, 0.16f)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp)
                ) {
                    CurioIcon(CurioIcons.Star, null, tint = sage, size = 20.dp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Category passport",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface
                )
                Text(
                    // The waiting stamps are named, because a stamp that has not
                    // landed is the only thing on this row that asks for anything.
                    if (awaitingStamp > 0) "$mastered of $total mastered · $awaitingStamp to stamp"
                    else "$mastered of $total lanes mastered",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CurioForwardArrow()
        }
    }
}
