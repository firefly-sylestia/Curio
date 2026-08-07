package com.curio.app.features.quests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.curio.app.data.AppPreferences
import com.curio.app.data.CategoryId
import com.curio.app.data.CurioCategories
import com.curio.app.data.CurioCategory
import com.curio.app.data.CurioPassport
import com.curio.app.data.CurioPet
import com.curio.app.data.CurioQuests
import com.curio.app.data.CurioQuests.DailyQuest
import com.curio.app.data.CurioQuests.QuestChain
import com.curio.app.data.CurioQuests.QuestStage
import com.curio.app.data.PromoMode
import com.curio.app.data.QuestGuide
import com.curio.app.navigation.CurioRoutes
import com.curio.app.navigation.navigateToQuestRoute
import com.curio.app.features.settings.SettingsHeroHeader
import com.curio.app.features.settings.SettingsHeroTotalHeight
import com.curio.app.ui.adaptive.isWide
import com.curio.app.ui.adaptive.wideContentEdgePadding
import com.curio.app.ui.adaptive.windowWidthSizeClass
import com.curio.app.ui.theme.isCurioDarkTheme
import com.curio.app.ui.components.CurioCardHeader
import com.curio.app.ui.components.CurioForwardArrow
import com.curio.app.ui.components.CurioSettingsCard
import com.curio.app.ui.components.CurioWatermarkBackdrop
import com.curio.app.ui.components.ScreenEntrance
import com.curio.app.ui.pet.CurioPetHeroCard
import com.curio.app.ui.theme.CurioColors
import com.curio.app.ui.theme.CurioGradients
import com.curio.app.ui.theme.CurioIcon
import com.curio.app.ui.theme.CurioIcons
import com.curio.app.ui.theme.themedAccent

/**
 * Quests & levels — Curio's gamification home (v8.0).
 *
 * Its own page, styled like the settings family: a compact torn rose hero
 * header on a watermark backdrop, then the rank card and the quest CHAINS.
 * Reads live reactive state from [CurioQuests], so badges pop the moment
 * they unlock and the current stage updates in place.
 *
 * Layout:
 *  1. Hero — the shared settings torn-banner header.
 *  2. Rank card — the XP bar, current rank, and "X of 50" ladder note.
 *  3. The current quest — the active stage across all chains, with a
 *     jump-to-it button when the stage has a screen.
 *  4. Quest chains — every chain (Tour, Deck, Discovery, Keepsakes, Shelf,
 *     Pin Board, Flame, Taste, Ladder) with its stages; the next stage is
 *     the hero, later stages preview as locked.
 *  5. Today's quests — the three daily quests with mini progress bars.
 *  6. Badge shelf — every chain stage as a badge, in a two-column grid.
 */
@Composable
fun QuestsScreen(navController: NavController) {
    // v7.107 — promo/demo-content mode shows the promotional sample XP (top
    // rank, Curio Sovereign) while ON; only the level card is demoed here.
    val promoOn = AppPreferences.promoModeState
    val xp = if (promoOn) PromoMode.DEMO_XP else CurioQuests.xpState
    val level = CurioQuests.levelForXp(xp)
    val (progress, nextThreshold) = CurioQuests.xpProgress(xp)
    val current = CurioQuests.currentQuest()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    // v8.5 — pet celebration: a daily claim bumps this key and the pet hero
    // hops once (spec §9). A soft confirmation haptic marks the reward
    // moment (spec §9.3 — light touch, respects device settings).
    var celebrate by remember { mutableStateOf(0) }
    // The pet's one-shot bubble for this visit (spec §10.7 — one per screen
    // visit). Fetched in a LaunchedEffect so the bubble's prefs write never
    // happens during composition.
    var petBubble by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        if (AppPreferences.petEnabledState) {
            petBubble = CurioPet.bubbleFor(context, "quests", CurioQuests.categoriesState)
        }
    }
    // The pet's accent — the least-engaged lane's tint, or the page coral
    // when every lane is already explored. (themedAccent is @Composable, so
    // only the prefs read is remembered.)
    val leastEngagedCat = remember { CurioPassport.leastEngaged(context) }
    val petAccent = leastEngagedCat?.themedAccent() ?: CurioColors.CoralBlush
    // v8.2 — the tour is offered ONCE and only from a tap on this page: the
    // first quest shows a prompt with a "No, thanks" option; a taken or
    // declined offer is never shown again, and the "Guided tour" Settings
    // toggle is the master switch. Any other quest (or a settled offer)
    // just navigates to the quest's screen.
    var showTourOffer by rememberSaveable { mutableStateOf(false) }
    val offerTour = current?.id == QuestGuide.firstQuestId &&
        AppPreferences.guideEnabledState && !AppPreferences.guideTourOfferedState
    val onQuestNavigate: (String) -> Unit = { route ->
        if (offerTour) showTourOffer = true
        else navController.navigateToQuestRoute(route)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Watermark backdrop — muted category glyphs behind the content
        // (the settings/profile language). Quests are category-neutral, so
        // the wildcard sparkle leads the collage.
        // Wide windows: the NavHost's full-bleed collage replaces the page's
        // own backdrop so there is ONE continuous collage, not a double.
        if (!windowWidthSizeClass().isWide) {
            CurioWatermarkBackdrop(
                activeCat = CurioCategories.byId(CategoryId.WILDCARD)
            )
        }
        // The hero is drawn LAST (on top of the scroll content): the quest
        // cards scroll UP and disappear behind the ragged tear instead of
        // clipping at a straight line — the same overlay construction as
        // every settings screen.
        ScreenEntrance {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = wideContentEdgePadding(), end = wideContentEdgePadding(), top = SettingsHeroTotalHeight + 10.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // v8.5 — Pet hero: the level card is replaced by the pet
                // companion (level + XP ring + growth line + speech bubble)
                // when the pet is enabled; the classic level card returns
                // when the toggle is off.
                item {
                    if (AppPreferences.petEnabledState) {
                        CurioPetHeroCard(
                            accent = petAccent,
                            bubbleText = petBubble,
                            onGo = onQuestNavigate,
                            celebrateKey = celebrate
                        )
                    } else {
                        LevelCard(
                            level = level,
                            xp = xp,
                            nextThreshold = nextThreshold,
                            progress = progress,
                            isMaxLevel = level >= CurioQuests.maxLevel
                        )
                    }
                }
                // v8.5 — Daily quests are FIRST under the hero: the page
                // answers "what can I do today" before anything else
                // (spec §3 + §4.1). Completing one fires the pet's
                // celebration hop.
                item {
                    DailyCard(
                        quests = CurioQuests.dailyQuestsFor(CurioQuests.todayEpochDay()),
                        // v8.3 — complete dailies are CLAIMED here (a tap
                        // grants the XP) and in-progress ones can Go straight
                        // to where the action happens.
                        onClaim = { questId ->
                            CurioQuests.claimDaily(context, questId)
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            celebrate++
                        },
                        onGo = onQuestNavigate
                    )
                }
                if (current != null) {
                    item {
                        CurrentQuestCard(
                            stage = current,
                            showTourCta = offerTour,
                            onNavigate = onQuestNavigate
                        )
                    }
                }
                // v8.5 — Category passport: every lane's stamp, tappable to
                // spin that lane (spec §6).
                item {
                    PassportCard(
                        onSpin = { slug ->
                            navController.navigate(CurioRoutes.spinWithCategory(slug)) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                // v8.2 — finished chains (every stage earned) are hidden so
                // the page focuses on what's left; the badge shelf below
                // still shows the full collection. v8.5 — chains collapse
                // by default so the page shows one primary next step
                // instead of every chain expanded (spec §3 + §4.1).
                val activeChains = CurioQuests.Chains.filter { chain ->
                    CurioQuests.chainProgress(chain) < chain.stages.size
                }
                items(activeChains.size) { index ->
                    ChainCard(
                        chain = activeChains[index],
                        onNavigate = onQuestNavigate
                    )
                }
                item {
                    BadgeShelf()
                }
            }
        }
        // Drawn on top of the scroll content — cards slide under the ragged
        // tear as they scroll up.
        SettingsHeroHeader(
            title = "Quests & levels",
            subtitle = "Grow your curiosity, one chain at a time",
            onBack = { navController.popBackStack() }
        )
    }

    // ── One-time tour offer (v8.2) — the first time the user taps the
    //    first quest, ask before launching the walkthrough. "Take the tour"
    //    starts it; "No, thanks" (or dismissing) marks the offer as seen so
    //    it never reappears — the first quest navigates normally afterwards.
    if (showTourOffer) {
        AlertDialog(
            onDismissRequest = {
                showTourOffer = false
                AppPreferences.setGuideTourOffered(context, true)
            },
            title = { Text("Take a quick tour?") },
            text = {
                Text(
                    "A small guide can walk you through every screen — Home, " +
                        "Spin, the Cabinet, Profile, Quests and Settings — so you " +
                        "know where everything lives. Takes about a minute."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showTourOffer = false
                    AppPreferences.setGuideTourOffered(context, true)
                    QuestGuide.start()
                }) { Text("Take the tour") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTourOffer = false
                    AppPreferences.setGuideTourOffered(context, true)
                }) { Text("No, thanks") }
            }
        )
    }
}

/** The rank card — big level badge, title, and the XP progress bar. */
@Composable
private fun LevelCard(level: Int, xp: Int, nextThreshold: Int, progress: Float, isMaxLevel: Boolean) {
    CurioSettingsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(CurioGradients.WildcardGradientStops.take(3))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$level",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Level $level · ${CurioQuests.levelTitle(level)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (isMaxLevel) "Curio Sovereign — the whole shelf is yours."
                    else "Rank $level of ${CurioQuests.maxLevel} · ${CurioQuests.maxLevel - level} to go",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CurioIcon(
                name = CurioIcons.WorkspacePremium,
                contentDescription = null,
                // Gold trophy — an earned rank reads better in warm gold
                // than the coral used everywhere else (v7.103).
                tint = CurioColors.ButterYellow,
                size = 30.dp
            )
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = CurioColors.CoralBlush,
            trackColor = CurioColors.CoralBlush.copy(alpha = 0.14f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isMaxLevel) "Curio Sovereign — the whole shelf is yours."
            else "$xp / $nextThreshold XP · ${(nextThreshold - xp).coerceAtLeast(0)} XP to Level ${level + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The single active quest across all chains — the hero of the page. */
@Composable
private fun CurrentQuestCard(
    stage: QuestStage,
    showTourCta: Boolean,
    onNavigate: (String) -> Unit
) {
    val roseHero = if (isCurioDarkTheme()) {
        CurioColors.HomeRosewoodDark
    } else {
        CurioColors.HomeRosewood
    }
    CurioSettingsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(CurioColors.CoralBlush),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = CurioIcons.TaskAlt,
                    contentDescription = null,
                    tint = Color.White,
                    size = 14.dp
                )
            }
            Text(
                // v8.5 — this is the single recommended next step (spec §3:
                // "One primary next step"), so the label says what it is.
                "RECOMMENDED NEXT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = roseHero
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stage.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            stage.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Hint: ${stage.hint}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        val done = CurioQuests.stageProgress(stage)
        val chain = CurioQuests.Chains.firstOrNull { it.stages.any { s -> s.id == stage.id } }
        // The very first quest ("First Spin") offers the one-time guided
        // tour instead of a plain jump — see QuestsScreen.onQuestNavigate.
        Surface(
            onClick = { stage.navRoute?.let(onNavigate) },
            shape = RoundedCornerShape(50),
            color = CurioColors.CoralBlush,
            enabled = stage.navRoute != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (showTourCta && stage.navRoute != null) "Take the tour · +${stage.xpReward} XP"
                else if (stage.navRoute != null) "Start · +${stage.xpReward} XP"
                else "In progress · ${done.coerceAtMost(stage.target)}/${stage.target}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(vertical = 9.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (chain != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "From the ${chain.title} chain — ${CurioQuests.chainProgress(chain)} of ${chain.stages.size} stages done",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/** One quest chain — its stages trail with the next one highlighted. */
@Composable
private fun ChainCard(
    chain: QuestChain,
    onNavigate: (String) -> Unit = {}
) {
    val chainDone = CurioQuests.chainProgress(chain)
    // v8.5 — chains collapse by default: the header + progress read at a
    // glance; tapping expands the stage trail. Keeps the page to one
    // primary next step instead of every chain's stages at once (spec §3).
    var expanded by rememberSaveable(chain.id) { mutableStateOf(false) }
    CurioSettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.verticalGradient(CurioGradients.cardGradient(CurioColors.CoralBlush))),
                contentAlignment = Alignment.Center
            ) {
                CurioIcon(
                    name = chain.glyph,
                    contentDescription = null,
                    tint = Color.White,
                    size = 20.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chain.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    chain.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "$chainDone/${chain.stages.size}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = if (chainDone == chain.stages.size) CurioColors.Sage else CurioColors.CoralBlush
            )
            // Expand/collapse chevron — rotates with the state.
            CurioIcon(
                name = if (expanded) CurioIcons.KeyboardArrowUp else CurioIcons.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse ${chain.title}" else "Expand ${chain.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 18.dp
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (chainDone.toFloat() / chain.stages.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50)),
            color = if (chainDone == chain.stages.size) CurioColors.Sage else CurioColors.CoralBlush,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        // v8.3 — the chain's NEXT actionable stage carries a Go/Start chip
        // too, so every chain points at what to do next (not just the
        // globally current quest). v8.5 — the stage trail is hidden until
        // the header is tapped.
        AnimatedVisibility(visible = expanded) {
            Column {
                val nextIndex = chain.stages.indexOfFirst { !CurioQuests.isStageDone(it) }
                chain.stages.forEachIndexed { index, stage ->
                    val done = CurioQuests.isStageDone(stage)
                    val isCurrent = !done && stage.id == CurioQuests.currentQuest()?.id
                    ChainStageRow(
                        index = index,
                        stage = stage,
                        done = done,
                        isCurrent = isCurrent,
                        isNext = index == nextIndex,
                        onNavigate = { stage.navRoute?.let(onNavigate) }
                    )
                }
            }
        }
    }
}

/** One stage row in a chain — number circle, title, and done/current state. */
@Composable
private fun ChainStageRow(
    index: Int,
    stage: QuestStage,
    done: Boolean,
    isCurrent: Boolean,
    isNext: Boolean,
    onNavigate: () -> Unit
) {
    val accent = when {
        done -> CurioColors.Sage
        isCurrent -> CurioColors.CoralBlush
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (done || isCurrent) 1f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                CurioIcon(CurioIcons.Check, null, tint = Color.White, size = 16.dp)
            } else {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stage.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isCurrent || done) FontWeight.ExtraBold else FontWeight.Medium
                ),
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (done) "Done · +${stage.xpReward} XP"
                else "+${stage.xpReward} XP",
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrent) CurioColors.CoralBlush else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // v8.3 — the chain's next actionable quest gets a Go/Start chip that
        // jumps the user to the quest's screen. The globally current quest is
        // the solid coral "Go"; the other chains' next quests are muted
        // "Start".
        if (!done && stage.navRoute != null && isNext) {
            Surface(
                onClick = onNavigate,
                shape = RoundedCornerShape(50),
                color = if (isCurrent) CurioColors.CoralBlush
                else CurioColors.CoralBlush.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (isCurrent) "Go" else "Start",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isCurrent) Color.White else CurioColors.CoralBlush
                    )
                    CurioForwardArrow(
                        if (isCurrent) "Go to quest" else "Start this quest",
                        tint = if (isCurrent) Color.White else CurioColors.CoralBlush,
                        size = 14.dp
                    )
                }
            }
        }
    }
}

/**
 * Where a daily quest's action happens — null when the quest has no single
 * destination screen (saving/bookmarking need an open topic or entry).
 */
private fun dailyGoRoute(kind: CurioQuests.DailyKind): String? = when (kind) {
    CurioQuests.DailyKind.SPIN -> CurioRoutes.SPIN
    // Exploring starts on a topic's reveal page — spinning is the fastest
    // way to land on one.
    CurioQuests.DailyKind.EXPLORE -> CurioRoutes.SPIN
    CurioQuests.DailyKind.PROFILE -> CurioRoutes.PROFILE
    else -> null
}

/** Today's three quests with mini progress bars (v8.3 — claimable rewards). */
@Composable
private fun DailyCard(
    quests: List<DailyQuest>,
    onClaim: (String) -> Unit,
    onGo: (String) -> Unit
) {
    CurioSettingsCard {
        CurioCardHeader(CurioIcons.EmojiEvents, "Today's quests", "Resets at midnight")
        Spacer(Modifier.height(2.dp))
        quests.forEach { quest ->
            val progress = CurioQuests.dailyProgressState[quest.kind.name] ?: 0
            val done = quest.id in CurioQuests.dailyAwardedState
            val fraction = (progress.toFloat() / quest.target).coerceIn(0f, 1f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (done) CurioColors.Sage.copy(alpha = 0.18f)
                            else CurioColors.CoralBlush.copy(alpha = 0.14f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = if (done) CurioIcons.Check else CurioIcons.TaskAlt,
                        contentDescription = null,
                        tint = if (done) CurioColors.Sage else CurioColors.CoralBlush,
                        size = 18.dp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        quest.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (done) FontWeight.ExtraBold else FontWeight.SemiBold
                        ),
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (done) CurioColors.Sage else CurioColors.CoralBlush,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Spacer(Modifier.width(6.dp))
                val claimable = !done && progress >= quest.target
                if (claimable) {
                    // v8.3 — Claim pill: a tap grants the quest's XP.
                    Surface(
                        onClick = { onClaim(quest.id) },
                        shape = RoundedCornerShape(50),
                        color = CurioColors.CoralBlush
                    ) {
                        Text(
                            "Claim +${quest.xpReward} XP",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                } else {
                    Text(
                        if (done) "Done" else "+${quest.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (done) CurioColors.Sage else CurioColors.CoralBlush
                    )
                    // v8.3 — Go chip on in-progress dailies: jump to the
                    // screen where this quest's action happens.
                    if (!done) {
                        dailyGoRoute(quest.kind)?.let { route ->
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                onClick = { onGo(route) },
                                shape = RoundedCornerShape(50),
                                color = CurioColors.CoralBlush.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        "Go",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = CurioColors.CoralBlush
                                    )
                                    CurioForwardArrow(
                                        "Go to quest",
                                        tint = CurioColors.CoralBlush,
                                        size = 12.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The badge shelf — every chain stage as a badge in a two-column grid. */
@Composable
private fun BadgeShelf() {
    val allStages = CurioQuests.allStages()
    val unlockedCount = allStages.count { CurioQuests.isStageDone(it) }
    CurioSettingsCard {
        CurioCardHeader(
            CurioIcons.EmojiEvents,
            "Badges",
            "$unlockedCount of ${allStages.size} earned"
        )
        Spacer(Modifier.height(4.dp))
        allStages.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { stage ->
                    BadgeTile(
                        stage = stage,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** One badge tile — glyph, title, and a tiny progress line. */
@Composable
private fun BadgeTile(
    stage: QuestStage,
    modifier: Modifier = Modifier
) {
    val unlocked = CurioQuests.isStageDone(stage)
    val progress = CurioQuests.stageProgress(stage)
    val accent = if (unlocked) CurioColors.Sage else CurioColors.CoralBlush
    val glyph = CurioQuests.Chains.firstOrNull { chain ->
        chain.stages.any { it.id == stage.id }
    }?.glyph ?: CurioIcons.EmojiEvents
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (unlocked) CurioColors.Sage.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            if (unlocked) CurioColors.Sage.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(accent.copy(alpha = if (unlocked) 1f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    CurioIcon(
                        name = if (unlocked) glyph else CurioIcons.StarOutline,
                        contentDescription = null,
                        tint = if (unlocked) Color.White else accent,
                        size = 18.dp
                    )
                }
                Text(
                    if (unlocked) "Unlocked" else "+${stage.xpReward} XP",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (unlocked) CurioColors.Sage else accent
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stage.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (unlocked) FontWeight.ExtraBold else FontWeight.SemiBold
                ),
                color = if (unlocked) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stage.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (progress.toFloat() / stage.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (unlocked) CurioColors.Sage else accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (unlocked) "Badge earned" else "$progress / ${stage.target}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * v8.5 — the category passport (spec §6): every lane as a tappable stamp.
 * Explored/mastered lanes show their stamp; unseen lanes look enticing and
 * route straight into that lane's Spin deck (spec §6.3).
 */
@Composable
private fun PassportCard(
    onSpin: (String) -> Unit
) {
    val context = LocalContext.current
    val cats = CurioCategories.visible
    val mastered = cats.count {
        CurioPassport.progress(context, it.id).stamp == CurioPassport.Stamp.MASTERED
    }
    CurioSettingsCard {
        CurioCardHeader(
            CurioIcons.Star,
            "Category passport",
            "$mastered of ${cats.size} lanes mastered"
        )
        Spacer(Modifier.height(4.dp))
        cats.chunked(3).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { cat ->
                    PassportStamp(
                        cat = cat,
                        modifier = Modifier.weight(1f),
                        onClick = { onSpin(cat.id.routeSlug) }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** One passport stamp — the lane's glyph, name and state. */
@Composable
private fun PassportStamp(
    cat: CurioCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val stamp = CurioPassport.progress(context, cat.id).stamp
    val accent = cat.themedAccent()
    val label: String
    val glyph: String
    val tint: Color
    when (stamp) {
        CurioPassport.Stamp.MASTERED -> {
            label = "Mastered"; glyph = CurioIcons.TaskAlt; tint = CurioColors.Sage
        }
        CurioPassport.Stamp.EXPLORED -> {
            label = "Explored"; glyph = CurioIcons.Check; tint = CurioColors.Sage
        }
        CurioPassport.Stamp.PEEKED -> {
            label = "Peeked"; glyph = CurioIcons.Star; tint = accent
        }
        CurioPassport.Stamp.UNSEEN -> {
            label = "New · spin!"; glyph = CurioIcons.StarOutline; tint = accent
        }
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = when (stamp) {
            CurioPassport.Stamp.MASTERED -> CurioColors.Sage.copy(alpha = 0.12f)
            CurioPassport.Stamp.UNSEEN -> accent.copy(alpha = 0.10f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            1.dp,
            if (stamp == CurioPassport.Stamp.UNSEEN) accent.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            CurioIcon(
                name = cat.iconGlyph,
                contentDescription = cat.displayName,
                tint = accent,
                size = 20.dp
            )
            Spacer(Modifier.height(4.dp))
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
